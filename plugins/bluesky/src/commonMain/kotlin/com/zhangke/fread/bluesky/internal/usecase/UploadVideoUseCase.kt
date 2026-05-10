package com.zhangke.fread.bluesky.internal.usecase

import app.bsky.video.JobStatus
import app.bsky.video.State
import com.atproto.server.GetServiceAuthQueryParams
import com.zhangke.framework.utils.AspectRatio
import com.zhangke.framework.utils.PlatformUri
import com.zhangke.framework.utils.VideoUtils
import com.zhangke.framework.utils.mapForErrorMessage
import com.zhangke.fread.bluesky.internal.account.BlueskyLoggedAccount
import com.zhangke.fread.bluesky.internal.client.BlueskyClient
import com.zhangke.fread.bluesky.internal.client.BlueskyClientManager
import com.zhangke.fread.bluesky.internal.utils.toResult
import com.zhangke.fread.common.utils.PlatformUriHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.model.Blob

class UploadVideoUseCase(
    private val clientManager: BlueskyClientManager,
    private val platformUriHelper: PlatformUriHelper,
) {

    companion object {
        private const val MAX_VIDEO_BYTES = 100L * 1024 * 1024
        private const val UPLOAD_LXM = "com.atproto.repo.uploadBlob"
        private const val JOB_STATUS_LXM = "app.bsky.video.getJobStatus"
        private const val SERVICE_AUTH_TTL_SECONDS = 30L * 60
        private const val POLL_INTERVAL_MS = 2_000L
        private const val POLL_TIMEOUT_MS = 5L * 60 * 1000
    }

    suspend operator fun invoke(
        account: BlueskyLoggedAccount,
        fileUri: PlatformUri,
        onStage: (suspend (UploadStage) -> Unit)? = null,
    ): Result<Pair<Blob, AspectRatio?>> = runCatching {
        val client = clientManager.getClient(account.locator)
        val file = platformUriHelper.read(fileUri)
            ?: throw RuntimeException("Could not load video file")
        if (file.size.bytes > MAX_VIDEO_BYTES) {
            val mb = file.size.bytes / (1024 * 1024)
            throw RuntimeException("Video too large: ${mb}MB (max 100MB)")
        }
        val aspect = VideoUtils().getVideoAspect(fileUri.toString())

        val pdsHost = extractPdsHost(account.didDoc)
            ?: throw RuntimeException("Could not determine PDS host for video upload")
        val pdsAud = Did("did:web:$pdsHost")
        val token = mintServiceAuth(client, pdsAud, UPLOAD_LXM)
            ?: throw RuntimeException("Could not authorize video upload")
        val statusToken = mintServiceAuth(client, pdsAud, JOB_STATUS_LXM)
            ?: throw RuntimeException("Could not authorize video status check")

        onStage?.invoke(UploadStage.Uploading(0f))
        val uploadResponse = client.uploadVideoStreamingCatching(
            sourceProvider = {
                platformUriHelper.openSource(fileUri)
                    ?: throw RuntimeException("Could not open video stream")
            },
            contentLength = file.size.bytes,
            mimeType = file.mimeType.ifEmpty { "video/mp4" },
            did = account.did,
            fileName = file.fileName.ifEmpty { "video.mp4" },
            serviceAuthToken = token,
            onBytesUploaded = if (onStage == null) null else { written, total ->
                if (total > 0) onStage.invoke(UploadStage.Uploading(written.toFloat() / total))
            },
        ).getOrElse {
            throw RuntimeException("Video upload failed: ${it.message}", it)
        }

        val initialJob = uploadResponse.jobStatus
        onStage?.invoke(UploadStage.Transcoding(initialJob.state.humanLabel(), initialJob.progress?.toInt()))
        val terminal = when {
            initialJob.state == State.JOBSTATECOMPLETED && initialJob.blob != null -> initialJob
            initialJob.state == State.JOBSTATECOMPLETED -> {
                // Reuse path (HTTP 409 from upload): the server already has a blob
                // for this exact byte stream from a prior job. Fetch it directly
                // instead of waiting on the poll cadence.
                runCatching {
                    client.getVideoJobStatusCatching(initialJob.jobId, statusToken).getOrThrow().jobStatus
                }.getOrElse {
                    throw RuntimeException("Failed to fetch existing video blob: ${it.message}", it)
                }
            }
            else -> withTimeout(POLL_TIMEOUT_MS) {
                pollUntilComplete(client, initialJob.jobId, statusToken, onStage)
            }
        }

        val blob = terminal.blob
            ?: throw RuntimeException(
                terminal.error?.let { "Video transcode failed: $it" }
                    ?: "Video transcode finished without a blob",
            )
        blob to aspect
    }.mapForErrorMessage("Upload video failed")

    private fun extractPdsHost(didDoc: JsonObject): String? {
        val services = (didDoc["service"] as? JsonArray) ?: return null
        for (entry in services) {
            val obj = entry as? JsonObject ?: continue
            val type = obj["type"]?.jsonPrimitive?.content
            if (type != "AtprotoPersonalDataServer") continue
            val endpoint = obj["serviceEndpoint"]?.jsonPrimitive?.content ?: continue
            return endpoint.substringAfter("://").substringBefore('/').ifEmpty { null }
        }
        return null
    }

    private suspend fun pollUntilComplete(
        client: BlueskyClient,
        jobId: String,
        statusToken: String,
        onStage: (suspend (UploadStage) -> Unit)?,
    ): JobStatus {
        while (true) {
            delay(POLL_INTERVAL_MS)
            val job = runCatching {
                client.getVideoJobStatusCatching(jobId, statusToken).getOrThrow()
            }.getOrElse {
                throw RuntimeException("Failed to poll video status: ${it.message}", it)
            }.jobStatus
            onStage?.invoke(UploadStage.Transcoding(job.state.humanLabel(), job.progress?.toInt()))
            when (job.state) {
                State.JOBSTATECOMPLETED -> return job
                State.JOBSTATEFAILED -> throw RuntimeException(
                    job.error?.let { "Video transcode failed: $it" }
                        ?: "Video transcode failed",
                )
                else -> Unit
            }
        }
    }

    private suspend fun mintServiceAuth(client: BlueskyClient, aud: Did, lxm: String): String? {
        return runCatching {
            client.getServiceAuth(
                GetServiceAuthQueryParams(
                    aud = aud,
                    exp = Clock.System.now().epochSeconds + SERVICE_AUTH_TTL_SECONDS,
                    lxm = Nsid(lxm),
                )
            ).toResult().getOrThrow().token
        }.getOrNull()
    }

    private fun State.humanLabel(): String = when (this) {
        is State.JOBSTATECOMPLETED -> "Done"
        is State.JOBSTATEFAILED -> "Failed"
        else -> when (val v = this.value.lowercase()) {
            "job_state_created", "created" -> "Queued"
            "job_state_encoding", "encoding" -> "Encoding"
            "job_state_encoded", "encoded" -> "Encoded"
            "job_state_scanning", "scanning" -> "Scanning"
            "job_state_scanned", "scanned" -> "Scanned"
            else -> v.removePrefix("job_state_").replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
    }
}
