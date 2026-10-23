package com.chaekchaek.app.ui.archive

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

internal interface BookshelfPlatform {
    fun readRecent(memberId: Long): Long?
    fun writeRecent(memberId: Long, shelfId: Long)
    suspend fun share(image: ImageBitmap)
    suspend fun save(image: ImageBitmap): Boolean
}

@Composable
internal expect fun rememberBookshelfPlatform(): BookshelfPlatform

/** GPU 캡처를 긴 이미지의 소프트웨어 캔버스에 합칠 수 있는 비트맵으로 바꾼다. */
internal expect fun ImageBitmap.forSoftwareCanvas(): ImageBitmap
