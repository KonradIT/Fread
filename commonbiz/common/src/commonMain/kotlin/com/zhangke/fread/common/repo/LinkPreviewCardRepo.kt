package com.zhangke.fread.common.repo

import com.zhangke.framework.architect.http.sharedHttpClient
import com.zhangke.framework.utils.LinkPreviewInfo
import com.zhangke.framework.utils.LinkPreviewUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.takeFrom

class LinkPreviewCardRepo {

    private val urlToInfoMap = mutableMapOf<String, LinkPreviewInfo>()

    suspend fun fetchPreviewInfo(url: String): Result<LinkPreviewInfo> = runCatching {
        val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        urlToInfoMap[normalizedUrl]?.let { return@runCatching it }
        val html = sharedHttpClient.get { url { takeFrom(normalizedUrl) } }.body<String>()
        val info = LinkPreviewUtils.fetchPreviewInfo(normalizedUrl, html)
            ?: throw IllegalStateException("Failed to fetch link preview info")
        urlToInfoMap[normalizedUrl] = info
        info
    }
}
