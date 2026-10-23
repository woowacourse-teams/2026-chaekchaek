package com.chaekchaek.app.ui.archive

import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
internal actual fun rememberBookshelfPlatform(): BookshelfPlatform {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<Pair<ByteArray, Continuation<Boolean>>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val request = pending
        pending = null
        if (request != null) {
            if (uri == null) request.second.resume(false)
            else runCatching {
                checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(request.first) }
            }.fold({ request.second.resume(true) }, { request.second.resumeWithException(it) })
        }
    }
    return remember(context, launcher) {
        object : BookshelfPlatform {
            private val preferences = context.getSharedPreferences("bookshelf-recent", 0)
            override fun readRecent(memberId: Long) = preferences.getLong("member-$memberId", -1).takeIf { it >= 0 }
            override fun writeRecent(memberId: Long, shelfId: Long) { preferences.edit().putLong("member-$memberId", shelfId).apply() }
            private fun png(image: ImageBitmap): ByteArray = ByteArrayOutputStream().use {
                check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
                it.toByteArray()
            }
            override suspend fun share(image: ImageBitmap) {
                val directory = File(context.cacheDir, "bookshelf-images").apply { mkdirs() }
                val file = File(directory, "bookshelf-${System.currentTimeMillis()}.png").apply { writeBytes(png(image)) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.bookshelf-images", file)
                val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, "책장 이미지 공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            override suspend fun save(image: ImageBitmap): Boolean {
                val bytes = png(image)
                return suspendCancellableCoroutine<Boolean> { continuation ->
                    check(pending == null)
                    pending = bytes to continuation
                    continuation.invokeOnCancellation { pending = null }
                    launcher.launch("책장.png")
                }
            }
        }
    }
}
