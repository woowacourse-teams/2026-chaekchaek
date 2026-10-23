@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.chaekchaek.app.ui.archive

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skia.Image
import org.jetbrains.skia.EncodedImageFormat
import platform.Foundation.*
import platform.Photos.*
import platform.UIKit.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun rememberBookshelfPlatform(): BookshelfPlatform = remember {
    object : BookshelfPlatform {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun readRecent(memberId: Long): Long? = if (defaults.objectForKey("bookshelf-recent-$memberId") == null) null else defaults.integerForKey("bookshelf-recent-$memberId")
        override fun writeRecent(memberId: Long, shelfId: Long) = defaults.setInteger(shelfId, "bookshelf-recent-$memberId")
        private fun nativeImage(image: ImageBitmap): UIImage {
            val bytes = checkNotNull(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)).bytes
            val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            return checkNotNull(UIImage(data = data))
        }
        override suspend fun share(image: ImageBitmap) {
            var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: error("공유 화면을 열 수 없어요")
            while (controller?.presentedViewController != null) controller = controller.presentedViewController
            val activity = UIActivityViewController(listOf(nativeImage(image)), null)
            activity.popoverPresentationController?.sourceView = controller?.view
            controller?.presentViewController(activity, animated = true, completion = null)
        }
        override suspend fun save(image: ImageBitmap): Boolean {
            val native = nativeImage(image)
            return suspendCancellableCoroutine<Boolean> { continuation ->
                PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                    PHAssetChangeRequest.creationRequestForAssetFromImage(native)
                }, completionHandler = { success, error ->
                    if (continuation.isActive) {
                        if (success) continuation.resume(true)
                        else continuation.resumeWithException(IllegalStateException(error?.localizedDescription ?: "이미지를 저장하지 못했어요"))
                    }
                })
            }
        }
    }
}

internal actual fun ImageBitmap.forSoftwareCanvas(): ImageBitmap = this
