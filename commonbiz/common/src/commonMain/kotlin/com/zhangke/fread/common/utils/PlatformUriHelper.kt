package com.zhangke.fread.common.utils

import androidx.compose.runtime.staticCompositionLocalOf
import com.zhangke.framework.utils.ContentProviderFile
import com.zhangke.framework.utils.PlatformUri

expect class PlatformUriHelper {

    suspend fun read(uri: PlatformUri): ContentProviderFile?

    suspend fun readBytes(uri: PlatformUri): ByteArray?

    /**
     * Opens an Okio [okio.Source] over [uri] without buffering its full contents into memory.
     * Caller is responsible for closing the source. Returns null if the URI cannot be opened.
     */
    suspend fun openSource(uri: PlatformUri): okio.Source?

    fun queryFileName(uri: PlatformUri): String?
}

val LocalPlatformUriHelper = staticCompositionLocalOf<PlatformUriHelper> {
    error("No PlatformUriHelper provided")
}