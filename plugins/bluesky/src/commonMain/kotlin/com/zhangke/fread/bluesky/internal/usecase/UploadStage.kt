package com.zhangke.fread.bluesky.internal.usecase

sealed interface UploadStage {
    data class Uploading(val fraction: Float) : UploadStage
    data class Transcoding(val label: String, val percent: Int?) : UploadStage
}
