package com.chaekchaek.app.ui.input

internal object TextInputLimit {
    fun takeCodePoints(value: String, maximum: Int): String {
        require(maximum >= 0)
        var end = 0
        var count = 0
        while (end < value.length && count < maximum) {
            end = value.nextCodePointEnd(end)
            count += 1
        }
        return value.substring(0, end)
    }

    fun applyChange(current: String, proposed: String, maximum: Int): String {
        require(maximum >= 0)
        if (proposed.codePointCount() <= maximum) return proposed

        val prefixEnd = commonPrefixEnd(current, proposed)
        val suffixLengths = commonSuffixLengths(current, proposed, prefixEnd)
        val unchangedCount =
            current.substring(0, prefixEnd).codePointCount() +
                current.substring(current.length - suffixLengths.current).codePointCount()
        val acceptedInsertion = takeCodePoints(
            proposed.substring(prefixEnd, proposed.length - suffixLengths.proposed),
            (maximum - unchangedCount).coerceAtLeast(0),
        )

        return buildString {
            append(proposed, 0, prefixEnd)
            append(acceptedInsertion)
            append(proposed, proposed.length - suffixLengths.proposed, proposed.length)
        }
    }

    private fun commonPrefixEnd(first: String, second: String): Int {
        var firstIndex = 0
        var secondIndex = 0
        while (firstIndex < first.length && secondIndex < second.length) {
            val firstEnd = first.nextCodePointEnd(firstIndex)
            val secondEnd = second.nextCodePointEnd(secondIndex)
            if (!first.regionMatches(firstIndex, second, secondIndex, firstEnd - firstIndex) ||
                firstEnd - firstIndex != secondEnd - secondIndex
            ) {
                break
            }
            firstIndex = firstEnd
            secondIndex = secondEnd
        }
        return firstIndex
    }

    private fun commonSuffixLengths(first: String, second: String, prefixEnd: Int): SuffixLengths {
        var firstLength = 0
        var secondLength = 0
        while (first.length - firstLength > prefixEnd && second.length - secondLength > prefixEnd) {
            val firstStart = first.previousCodePointStart(first.length - firstLength)
            val secondStart = second.previousCodePointStart(second.length - secondLength)
            val firstWidth = first.length - firstLength - firstStart
            val secondWidth = second.length - secondLength - secondStart
            if (firstWidth != secondWidth || !first.regionMatches(firstStart, second, secondStart, firstWidth)) break
            firstLength += firstWidth
            secondLength += secondWidth
        }
        return SuffixLengths(current = firstLength, proposed = secondLength)
    }

    private fun String.codePointCount(): Int {
        var index = 0
        var count = 0
        while (index < length) {
            index = nextCodePointEnd(index)
            count += 1
        }
        return count
    }

    private fun String.nextCodePointEnd(index: Int): Int =
        if (index + 1 < length && this[index].isHighSurrogate() && this[index + 1].isLowSurrogate()) index + 2 else index + 1

    private fun String.previousCodePointStart(end: Int): Int =
        if (end >= 2 && this[end - 2].isHighSurrogate() && this[end - 1].isLowSurrogate()) end - 2 else end - 1

    private fun Char.isHighSurrogate(): Boolean = this in '\uD800'..'\uDBFF'

    private fun Char.isLowSurrogate(): Boolean = this in '\uDC00'..'\uDFFF'

    private data class SuffixLengths(val current: Int, val proposed: Int)
}
