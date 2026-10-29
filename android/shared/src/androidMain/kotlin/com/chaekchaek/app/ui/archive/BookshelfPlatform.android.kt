package com.chaekchaek.app.ui.archive

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun rememberBookshelfPlatform(): BookshelfPlatform {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<ByteArray, CancellableContinuation<Boolean>>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val request = pending
        pending = null
        if (request != null && request.second.isActive) {
            if (uri == null) request.second.resume(false)
            else runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(request.first) }
            }.fold({ request.second.resume(true) }, { request.second.resumeWithException(it) })
        }
    }
    var pendingShare by remember { mutableStateOf<CancellableContinuation<Unit>?>(null) }
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val continuation = pendingShare
        pendingShare = null
        if (continuation?.isActive == true) continuation.resume(Unit)
    }
    return remember(context, launcher, shareLauncher) {
        object : BookshelfPlatform {
            private val preferences = context.getSharedPreferences("bookshelf-recent", 0)
            override fun readRecent(memberId: Long) = preferences.getLong("member-$memberId", -1).takeIf { it >= 0 }
            override fun writeRecent(memberId: Long, shelfId: Long) { preferences.edit().putLong("member-$memberId", shelfId).apply() }
            private fun png(image: ImageBitmap): ByteArray = ByteArrayOutputStream().use {
                check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
                it.toByteArray()
            }
            override suspend fun share(image: ImageBitmap) {
                val file = withContext(Dispatchers.Default) {
                    val directory = File(context.cacheDir, "bookshelf-images").apply { mkdirs() }
                    File(directory, "bookshelf-${System.currentTimeMillis()}.png").apply { writeBytes(png(image)) }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.bookshelf-images", file)
                val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                suspendCancellableCoroutine<Unit> { continuation ->
                    check(pendingShare == null)
                    pendingShare = continuation
                    continuation.invokeOnCancellation { if (pendingShare === continuation) pendingShare = null }
                    try {
                        shareLauncher.launch(Intent.createChooser(intent, "책장 이미지 공유"))
                    } catch (error: Throwable) {
                        pendingShare = null
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
            override suspend fun save(image: ImageBitmap): Boolean {
                val bytes = withContext(Dispatchers.Default) { png(image) }
                return suspendCancellableCoroutine<Boolean> { continuation ->
                    check(pending == null)
                    pending = bytes to continuation
                    continuation.invokeOnCancellation { if (pending?.second === continuation) pending = null }
                    try {
                        launcher.launch("책장.png")
                    } catch (error: Throwable) {
                        pending = null
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            }
        }
    }
}

internal actual fun ImageBitmap.forSoftwareCanvas(): ImageBitmap {
    val bitmap = asAndroidBitmap()
    return if (bitmap.config == Bitmap.Config.HARDWARE) checkNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)).asImageBitmap() else this
}
