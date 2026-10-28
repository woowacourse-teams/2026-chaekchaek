package com.chaekchaek.app.ui.archive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Approved HTML spine proportions shared by own, public and exported shelves. */
internal fun bookshelfSpineHeight(index: Int, scale: Float = 1.4f): Dp =
    listOf(156.928f, 177f, 144.155f, 135.031f, 157.84f, 135.031f, 159.665f, 173.351f, 148.716f, 133.206f, 149.629f, 166.964f, 125.907f, 169.701f)[index.mod(14)].times(scale).dp

internal val BookshelfSpineRowHeight = 269.dp
