package com.chaekchaek.app.ui.input

internal object TextInputLimit {
    fun takeCodePoints(value: String, maximum: Int, digitsOnly: Boolean = false): String = take(value, maximum, false, digitsOnly)

    fun takeUtf16(value: String, maximum: Int, digitsOnly: Boolean = false): String = take(value, maximum, true, digitsOnly)

    private fun take(value: String, maximum: Int, utf16: Boolean, digitsOnly: Boolean, start: Int = 0, endExclusive: Int = value.length): String {
        require(maximum >= 0)
        return buildString {
            var index = start
            var count = 0
            while (index < endExclusive && count < maximum) {
                val end = value.nextCodePointEnd(index)
                val width = end - index
                val valid = width == 2 || (!value[index].isHighSurrogate() && !value[index].isLowSurrogate())
                val cost = if (utf16) width else 1
                if (valid && (!digitsOnly || value[index].isDigit())) {
                    if (count + cost > maximum) break
                    append(value, index, end)
                    count += cost
                }
                index = end
            }
        }
    }

    fun applyChange(current: String, proposed: String, maximum: Int, digitsOnly: Boolean = false): String =
        applyChange(current, proposed, maximum, false, digitsOnly)

    fun applyUtf16Change(current: String, proposed: String, maximum: Int, digitsOnly: Boolean = false): String =
        applyChange(current, proposed, maximum, true, digitsOnly)

    private fun applyChange(current: String, proposed: String, maximum: Int, utf16: Boolean, digitsOnly: Boolean): String {
        require(maximum >= 0)
        val boundedCurrent = take(current, maximum, utf16, digitsOnly)
        val boundedProposed = take(proposed, maximum, utf16, digitsOnly)
        if (boundedProposed == proposed) return proposed

        val prefixEnd = commonPrefixEnd(boundedCurrent, proposed)
        val suffixLengths = commonSuffixLengths(boundedCurrent, proposed, prefixEnd)
        val unchangedPrefix = boundedCurrent.substring(0, prefixEnd)
        val unchangedSuffix = boundedCurrent.substring(boundedCurrent.length - suffixLengths.current)
        fun count(value: String): Int = if (utf16) value.length else value.codePointCount()
        val remaining = (maximum - count(unchangedPrefix) - count(unchangedSuffix)).coerceAtLeast(0)
        val acceptedInsertion = take(
            proposed, remaining, utf16, digitsOnly, prefixEnd, proposed.length - suffixLengths.proposed,
        )
        return unchangedPrefix + acceptedInsertion + unchangedSuffix
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
