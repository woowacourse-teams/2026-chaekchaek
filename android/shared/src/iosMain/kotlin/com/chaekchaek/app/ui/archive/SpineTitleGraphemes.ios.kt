package com.chaekchaek.app.ui.archive

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSString

@OptIn(ExperimentalForeignApi::class)
internal actual fun spineTitleGraphemes(title: String): List<String> {
    val text = title as NSString
    return buildList {
        var start = 0
        while (start < title.length) {
            val end = text.rangeOfComposedCharacterSequenceAtIndex(start.toULong()).useContents {
                (location + length).toInt()
            }
            add(title.substring(start, end))
            start = end
        }
    }
}
