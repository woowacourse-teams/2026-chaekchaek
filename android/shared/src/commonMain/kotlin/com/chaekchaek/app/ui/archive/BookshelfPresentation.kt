package com.chaekchaek.app.ui.archive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** All shelves share one book height. Only the source image ratio changes thickness. */
internal val BookshelfSpineHeight = 220.dp
internal const val BookshelfSpineDefaultScale = 1.4f

internal fun scaledBookshelfSpineHeight(scale: Float): Dp =
    BookshelfSpineHeight * (scale / BookshelfSpineDefaultScale)

internal fun bookshelfSpineThickness(
    height: Dp,
    intrinsicWidth: Float,
    intrinsicHeight: Float,
    fallback: Dp,
): Dp = if (
    intrinsicWidth.isFinite() && intrinsicHeight.isFinite() && intrinsicWidth > 0f && intrinsicHeight > 0f
) {
    height * (intrinsicWidth / intrinsicHeight)
} else {
    fallback
}

internal val BookshelfSpineRowHeight = 269.dp
