package com.zhangke.fread.common.alttext

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import kotlin.math.max

@OptIn(ExperimentalForeignApi::class)
actual fun resizeAndJpegBase64(
    bytes: ByteArray,
    maxLongestSide: Int,
    quality: Int,
): String {
    if (bytes.isEmpty()) error("Image decode failed")
    val sourceData = bytes.toNSData()
    val source = UIImage(data = sourceData) ?: error("Image decode failed")

    val (sourceWidth, sourceHeight) = source.size.useContents { width to height }
    val maxDim = max(sourceWidth, sourceHeight)
    val scale = if (maxDim > maxLongestSide.toDouble()) {
        maxLongestSide.toDouble() / maxDim
    } else {
        1.0
    }
    val targetWidth = sourceWidth * scale
    val targetHeight = sourceHeight * scale

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(targetWidth, targetHeight),
        false,
        1.0,
    )
    source.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resized = UIGraphicsGetImageFromCurrentImageContext() ?: source
    UIGraphicsEndImageContext()

    val compressionQuality = (quality.toDouble() / 100.0).coerceIn(0.0, 1.0)
    val jpegData = UIImageJPEGRepresentation(resized, compressionQuality)
        ?: error("JPEG encode failed")

    return jpegData.base64EncodedStringWithOptions(0uL)
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
