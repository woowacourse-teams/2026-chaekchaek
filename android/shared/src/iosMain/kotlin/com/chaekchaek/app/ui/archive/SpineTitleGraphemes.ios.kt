package com.chaekchaek.app.ui.archive

internal actual fun spineTitleGraphemes(title: String): List<String> {
    return buildList {
        var start = 0
        while (start < title.length) {
            var end = title.nextCodePointIndex(start)
            var previous = title.codePointAt(start)
            var regionalIndicators = if (previous.isRegionalIndicator()) 1 else 0
            while (end < title.length) {
                val current = title.codePointAt(end)
                val joinsPrevious =
                    current.isGraphemeExtension() ||
                        current == ZERO_WIDTH_JOINER ||
                        previous == ZERO_WIDTH_JOINER ||
                        previous.isHangulJamo() && current.isHangulJamo() ||
                        previous.isRegionalIndicator() &&
                        current.isRegionalIndicator() &&
                        regionalIndicators % 2 == 1
                if (!joinsPrevious) break
                end = title.nextCodePointIndex(end)
                regionalIndicators = if (current.isRegionalIndicator()) regionalIndicators + 1 else 0
                previous = current
            }
            add(title.substring(start, end))
            start = end
        }
    }
}

private const val ZERO_WIDTH_JOINER = 0x200D

private fun String.codePointAt(index: Int): Int {
    val first = this[index].code
    if (first !in 0xD800..0xDBFF || index + 1 >= length) return first
    val second = this[index + 1].code
    if (second !in 0xDC00..0xDFFF) return first
    return 0x10000 + ((first - 0xD800) shl 10) + (second - 0xDC00)
}

private fun String.nextCodePointIndex(index: Int): Int =
    index + if (this[index].code in 0xD800..0xDBFF &&
        index + 1 < length && this[index + 1].code in 0xDC00..0xDFFF
    ) 2 else 1

private fun Int.isRegionalIndicator(): Boolean = this in 0x1F1E6..0x1F1FF

private fun Int.isHangulJamo(): Boolean =
    this in 0x1100..0x11FF || this in 0xA960..0xA97F || this in 0xD7B0..0xD7FF

private fun Int.isGraphemeExtension(): Boolean =
    this in 0x0300..0x036F ||
        this in 0x1AB0..0x1AFF ||
        this in 0x1DC0..0x1DFF ||
        this in 0x20D0..0x20FF ||
        this in 0xFE00..0xFE0F ||
        this in 0xFE20..0xFE2F ||
        this in 0x1F3FB..0x1F3FF ||
        this in 0xE0020..0xE007F ||
        this in 0xE0100..0xE01EF
