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
