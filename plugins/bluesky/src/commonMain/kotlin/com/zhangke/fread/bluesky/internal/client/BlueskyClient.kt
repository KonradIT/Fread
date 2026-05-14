package com.zhangke.fread.bluesky.internal.client

import app.bsky.actor.GetPreferencesResponse
import app.bsky.actor.GetProfileQueryParams
import app.bsky.actor.GetProfilesQueryParams
import app.bsky.actor.GetProfilesResponse
import app.bsky.actor.ProfileView
import app.bsky.actor.ProfileViewDetailed
import app.bsky.actor.PutPreferencesRequest
import app.bsky.actor.SearchActorsQueryParams
import app.bsky.actor.SearchActorsResponse
import app.bsky.feed.GetActorFeedsQueryParams
import app.bsky.feed.GetActorFeedsResponse
import app.bsky.feed.GetActorLikesQueryParams
import app.bsky.feed.GetActorLikesResponse
import app.bsky.feed.GetAuthorFeedQueryParams
import app.bsky.feed.GetAuthorFeedResponse
import app.bsky.feed.GetFeedGeneratorsQueryParams
import app.bsky.feed.GetFeedGeneratorsResponse
import app.bsky.feed.GetFeedQueryParams
import app.bsky.feed.GetFeedResponse
import app.bsky.feed.GetLikesQueryParams
import app.bsky.feed.GetListFeedQueryParams
import app.bsky.feed.GetListFeedResponse
import app.bsky.feed.GetPostThreadQueryParams
import app.bsky.feed.GetPostThreadResponse
import app.bsky.feed.GetPostsQueryParams
import app.bsky.feed.GetPostsResponse
import app.bsky.feed.GetQuotesQueryParams
import app.bsky.feed.PostView
import app.bsky.feed.GetRepostedByQueryParams
import app.bsky.feed.GetSuggestedFeedsQueryParams
import app.bsky.feed.GetSuggestedFeedsResponse
import app.bsky.feed.GetTimelineQueryParams
import app.bsky.feed.GetTimelineResponse
import app.bsky.feed.SearchPostsQueryParams
import app.bsky.feed.SearchPostsResponse
import app.bsky.graph.GetBlocksQueryParams
import app.bsky.graph.GetFollowersQueryParams
import app.bsky.graph.GetFollowsQueryParams
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetListResponse
import app.bsky.graph.GetListsQueryParams
import app.bsky.graph.GetListsResponse
import app.bsky.graph.GetMutesQueryParams
import app.bsky.graph.MuteActorRequest
import app.bsky.graph.UnmuteActorRequest
import app.bsky.notification.ListNotificationsQueryParams
import app.bsky.notification.ListNotificationsResponse
import app.bsky.notification.UpdateSeenRequest
import app.bsky.unspecced.GetPopularFeedGeneratorsQueryParams
import app.bsky.unspecced.GetPopularFeedGeneratorsResponse
import com.atproto.repo.ApplyWritesRequest
import com.atproto.repo.ApplyWritesResponse
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.CreateRecordResponse
import com.atproto.repo.DeleteRecordRequest
import com.atproto.repo.DeleteRecordResponse
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.GetRecordResponse
import com.atproto.repo.PutRecordRequest
import com.atproto.repo.PutRecordResponse
import app.bsky.video.GetJobStatusResponse
import app.bsky.video.JobStatus
import app.bsky.video.UploadVideoResponse
import com.atproto.repo.UploadBlobResponse
import com.atproto.server.CreateSessionRequest
import com.atproto.server.CreateSessionResponse
import com.atproto.server.RefreshSessionResponse
import com.zhangke.framework.network.FormalBaseUrl
import com.zhangke.framework.utils.exceptionOrThrow
import com.zhangke.fread.bluesky.internal.account.BlueskyLoggedAccount
import com.zhangke.fread.bluesky.internal.utils.toResult
import com.zhangke.fread.status.model.PagedData
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import okio.buffer
import sh.christian.ozone.BlueskyApi
import sh.christian.ozone.XrpcBlueskyApi
import sh.christian.ozone.api.AtIdentifier
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Uri
import sh.christian.ozone.api.response.AtpResponse

class BlueskyClient(
    val baseUrl: FormalBaseUrl,
    private val engine: HttpClientEngine,
    val json: Json,
    val loggedAccountProvider: suspend () -> BlueskyLoggedAccount?,
    val newSessionUpdater: suspend (RefreshSessionResponse) -> Unit,
    val onLoginRequest: suspend () -> Unit,
    private val labelersCache: BlueskyLabelersCache,
) : BlueskyApi by XrpcBlueskyApi(
    createBlueskyHttpClient(
        engine,
        json,
        baseUrl.toString(),
        loggedAccountProvider,
        newSessionUpdater,
        onLoginRequest,
        labelersCache,
    )
) {

    suspend fun createSessionCatching(request: CreateSessionRequest): Result<CreateSessionResponse> {
        return runCatching { createSession(request) }.toResult()
    }

    suspend fun refreshSessionCatching(): Result<RefreshSessionResponse> {
        return kotlin.runCatching { refreshSession() }.toResult()
    }

    suspend fun getProfileCatching(request: GetProfileQueryParams): Result<ProfileViewDetailed> {
        return runCatching { getProfile(request) }.toResult()
    }

    suspend fun getProfileCatching(did: String): Result<ProfileViewDetailed> {
        return runCatching { getProfile(GetProfileQueryParams(Did(did))) }.toResult()
    }

    suspend fun getProfilesCatching(request: GetProfilesQueryParams): Result<GetProfilesResponse> {
        return runCatching { getProfiles(request) }.toResult()
    }

    suspend fun getTimelineCatching(request: GetTimelineQueryParams): Result<GetTimelineResponse> {
        return runCatching { getTimeline(request) }.toResult()
    }

    suspend fun getPreferencesCatching(): Result<GetPreferencesResponse> {
        return runCatching { getPreferences() }.toResult()
    }

    suspend fun putPreferencesCatching(request: PutPreferencesRequest): Result<Unit> {
        return runCatching { putPreferences(request) }.toResult()
    }

    suspend fun getFeedGeneratorsCatching(params: GetFeedGeneratorsQueryParams): Result<GetFeedGeneratorsResponse> {
        return runCatching { getFeedGenerators(params) }.toResult()
    }

    suspend fun getFeedCatching(params: GetFeedQueryParams): Result<GetFeedResponse> {
        return runCatching { getFeed(params) }.toResult()
    }

    suspend fun getPopularFeedGeneratorsUnspeccedCatching(params: GetPopularFeedGeneratorsQueryParams): Result<GetPopularFeedGeneratorsResponse> {
        return runCatching { getPopularFeedGeneratorsUnspecced(params) }.toResult()
    }

    suspend fun getActorFeedsCatching(request: GetActorFeedsQueryParams): Result<GetActorFeedsResponse> {
        return runCatching { getActorFeeds(request) }.toResult()
    }

    suspend fun getAuthorFeedCatching(request: GetAuthorFeedQueryParams): Result<GetAuthorFeedResponse> {
        return runCatching { getAuthorFeed(request) }.toResult()
    }

    suspend fun getSuggestedFeedsCatching(params: GetSuggestedFeedsQueryParams): Result<GetSuggestedFeedsResponse> {
        return runCatching { getSuggestedFeeds(params) }.toResult()
    }

    suspend fun getListsCatching(request: GetListsQueryParams): Result<GetListsResponse> {
        return runCatching { getLists(request) }.toResult()
    }

    suspend fun getListCatching(params: GetListQueryParams): Result<GetListResponse> {
        return runCatching { getList(params) }.toResult()
    }

    suspend fun getListFeedCatching(params: GetListFeedQueryParams): Result<GetListFeedResponse> {
        return runCatching { getListFeed(params) }.toResult()
    }

    suspend fun getRecordCatching(params: GetRecordQueryParams): Result<GetRecordResponse> {
        return runCatching { getRecord(params) }.toResult()
    }

    suspend fun createRecordCatching(params: CreateRecordRequest): Result<CreateRecordResponse> {
        return runCatching { createRecord(params) }.toResult()
    }

    suspend fun putRecordCatching(params: PutRecordRequest): Result<PutRecordResponse> {
        return runCatching { putRecord(params) }.toResult()
    }

    suspend fun deleteRecordCatching(params: DeleteRecordRequest): Result<DeleteRecordResponse> {
        return runCatching { deleteRecord(params) }.toResult()
    }

    suspend fun getPostThreadCatching(params: GetPostThreadQueryParams): Result<GetPostThreadResponse> {
        return runCatching { getPostThread(params) }.toResult()
    }

    suspend fun getPostsCatching(params: GetPostsQueryParams): Result<GetPostsResponse> {
        return runCatching { getPosts(params) }.toResult()
    }

    suspend fun searchPostsCatching(params: SearchPostsQueryParams): Result<SearchPostsResponse> {
        return runCatching { searchPosts(params) }.toResult()
    }

    suspend fun searchActorsCatching(params: SearchActorsQueryParams): Result<SearchActorsResponse> {
        return runCatching { searchActors(params) }.toResult()
    }

    suspend fun getActorLikesCatching(params: GetActorLikesQueryParams): Result<GetActorLikesResponse> {
        return runCatching { getActorLikes(params) }.toResult()
    }

    suspend fun listNotificationsCatching(params: ListNotificationsQueryParams): Result<ListNotificationsResponse> {
        return runCatching { listNotifications(params) }.toResult()
    }

    suspend fun updateSeenCatching(request: UpdateSeenRequest): Result<Unit> {
        return runCatching { updateSeen(request) }.toResult()
    }

    suspend fun muteActorCatching(actor: AtIdentifier): Result<Unit> {
        return runCatching { muteActor(MuteActorRequest(actor)) }.toResult()
    }

    suspend fun unmuteActorCatching(actor: AtIdentifier): Result<Unit> {
        return runCatching { unmuteActor(UnmuteActorRequest(actor)) }.toResult()
    }

    suspend fun getFollowsCatching(params: GetFollowsQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getFollows(params) }.toResult()
            .map { PagedData(list = it.follows, cursor = it.cursor) }
    }

    suspend fun getFollowersCatching(params: GetFollowersQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getFollowers(params) }.toResult()
            .map { PagedData(list = it.followers, cursor = it.cursor) }
    }

    suspend fun getMutesCatching(params: GetMutesQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getMutes(params) }.toResult()
            .map { PagedData(list = it.mutes, cursor = it.cursor) }
    }

    suspend fun getBlocksCatching(params: GetBlocksQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getBlocks(params) }.toResult()
            .map { PagedData(list = it.blocks, cursor = it.cursor) }
    }

    suspend fun getLikesCatching(params: GetLikesQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getLikes(params) }.toResult()
            .map { data -> PagedData(list = data.likes.map { it.actor }, cursor = data.cursor) }
    }

    suspend fun getRepostedCatching(params: GetRepostedByQueryParams): Result<PagedData<ProfileView>> {
        return runCatching { getRepostedBy(params) }.toResult()
            .map { data -> PagedData(list = data.repostedBy, cursor = data.cursor) }
    }

    suspend fun getQuotesCatching(params: GetQuotesQueryParams): Result<PagedData<PostView>> {
        return runCatching { getQuotes(params) }.toResult()
            .map { data -> PagedData(list = data.posts, cursor = data.cursor) }
    }

    suspend fun uploadBlobCatching(data: ByteArray): Result<UploadBlobResponse> {
        return runCatching { uploadBlob(data) }.toResult()
    }

    private val streamingHttpClient: HttpClient by lazy {
        HttpClient(engine) {
            install(Logging) { level = LogLevel.INFO }
            expectSuccess = false
        }
    }

    /**
     * Streams a video upload to the Bluesky video service (video.bsky.app).
     * The body is sent without buffering the file into memory: the supplied [sourceProvider]
     * is invoked once writing begins and is closed after the channel finishes.
     *
     * [serviceAuthToken] must be obtained via com.atproto.server.getServiceAuth with
     * `aud = did:web:video.bsky.app` and `lxm = app.bsky.video.uploadVideo`.
     */
    suspend fun uploadVideoStreamingCatching(
        sourceProvider: suspend () -> okio.Source,
        contentLength: Long,
        mimeType: String,
        did: String,
        fileName: String,
        serviceAuthToken: String,
        onBytesUploaded: (suspend (Long, Long) -> Unit)? = null,
    ): Result<UploadVideoResponse> = runCatching {
        val response = streamingHttpClient.post {
            url {
                takeFrom("https://video.bsky.app/xrpc/app.bsky.video.uploadVideo")
                parameters.append("did", did)
                parameters.append("name", fileName)
            }
            header(HttpHeaders.Authorization, "Bearer $serviceAuthToken")
            contentType(ContentType.parse(mimeType))
            setBody(
                StreamingBody(
                    sourceProvider = sourceProvider,
                    contentLength = contentLength,
                    contentType = ContentType.parse(mimeType),
                    onBytesUploaded = onBytesUploaded,
                )
            )
        }
        val text = response.bodyAsText()
        // 409 with error=already_exists: the video has been processed before.
        // The body is a flat JobStatus (no `jobStatus` wrapper) carrying the prior
        // jobId. Reuse it — the caller's poll loop will resolve the blob via
        // getJobStatus and skip the upload entirely.
        if (response.status.value == 409) {
            val flat = json.parseToJsonElement(text).jsonObject
            val jobStatus = json.decodeFromJsonElement(JobStatus.serializer(), flat)
            return@runCatching UploadVideoResponse(jobStatus = jobStatus)
        }
        if (!response.status.isSuccess()) {
            throw RuntimeException("uploadVideo failed (${response.status.value}): $text")
        }
        json.decodeFromString(UploadVideoResponse.serializer(), text)
    }

    /**
     * Polls transcode job status against the video service directly. The user's
     * PDS does not implement `app.bsky.video.getJobStatus` (returns 501); this
     * endpoint is hosted by `video.bsky.app` and authenticated with a service
     * auth token signed by the user's PDS.
     */
    suspend fun getVideoJobStatusCatching(
        jobId: String,
        serviceAuthToken: String,
    ): Result<GetJobStatusResponse> = runCatching {
        val response = streamingHttpClient.get {
            url {
                takeFrom("https://video.bsky.app/xrpc/app.bsky.video.getJobStatus")
                parameters.append("jobId", jobId)
            }
            header(HttpHeaders.Authorization, "Bearer $serviceAuthToken")
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw RuntimeException("getJobStatus failed (${response.status.value}): $text")
        }
        json.decodeFromString(GetJobStatusResponse.serializer(), text)
    }

    suspend fun applyWritesCatching(request: ApplyWritesRequest): Result<ApplyWritesResponse> {
        return runCatching { applyWrites(request) }.toResult()
    }

    suspend fun searchPostsByUri(uri: String): Result<SearchPostsResponse> {
        return runCatching {
            searchPosts(SearchPostsQueryParams(q = "", url = Uri(uri)))
        }.toResult()
    }

    private fun <T : Any> Result<AtpResponse<T>>.toResult(): Result<T> {
        if (this.isFailure) return Result.failure(this.exceptionOrThrow())
        return this.getOrThrow().toResult()
    }
}

private class StreamingBody(
    private val sourceProvider: suspend () -> okio.Source,
    override val contentLength: Long,
    override val contentType: ContentType,
    private val onBytesUploaded: (suspend (Long, Long) -> Unit)? = null,
) : OutgoingContent.WriteChannelContent() {
    override suspend fun writeTo(channel: ByteWriteChannel) {
        var written = 0L
        sourceProvider().buffer().use { src ->
            val chunk = ByteArray(64 * 1024)
            while (true) {
                val read = src.read(chunk, 0, chunk.size)
                if (read == -1) break
                channel.writeFully(chunk, 0, read)
                written += read
                onBytesUploaded?.invoke(written, contentLength)
            }
        }
    }
}

private fun createBlueskyHttpClient(
    engine: HttpClientEngine,
    json: Json,
    baseUrl: String,
    accountProvider: suspend () -> BlueskyLoggedAccount?,
    newSessionUpdater: suspend (RefreshSessionResponse) -> Unit,
    onLoginRequest: suspend () -> Unit,
    labelersCache: BlueskyLabelersCache,
): HttpClient {
    return HttpClient(engine) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(DefaultRequest) {
            val hostUrl = Url(baseUrl)
            url.protocol = hostUrl.protocol
            url.host = hostUrl.host
            url.port = hostUrl.port
        }
        install(XrpcAuthPlugin) {
            this.json = json
            this.accountProvider = accountProvider
            this.newSessionUpdater = newSessionUpdater
            this.onLoginRequest = onLoginRequest
        }
        install(AtProtoProxyPlugin)
        install(LabelersHeaderPlugin) {
            this.cache = labelersCache
        }
        expectSuccess = false
    }
}
