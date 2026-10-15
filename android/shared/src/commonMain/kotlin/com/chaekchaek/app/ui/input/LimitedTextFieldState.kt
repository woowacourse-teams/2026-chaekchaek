package com.chaekchaek.app.ui.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.maxTextLength
import androidx.compose.ui.text.TextRange

/** 입력, 초기값, 저장 및 복원이 같은 상한을 사용한다. 기존 서버 검증의 UTF-16 상한도 유지한다. */
internal data class TextInputPolicy(
    val maximum: Int,
    val utf16: Boolean = false,
    val digitsOnly: Boolean = false,
) {
    fun limit(value: String): String =
        if (utf16) TextInputLimit.takeUtf16(value, maximum, digitsOnly)
        else TextInputLimit.takeCodePoints(value, maximum, digitsOnly)

    fun applyChange(current: String, proposed: String): String =
        if (utf16) TextInputLimit.applyUtf16Change(current, proposed, maximum, digitsOnly)
        else TextInputLimit.applyChange(current, proposed, maximum, digitsOnly)

    val inputTransformation: InputTransformation = object : InputTransformation {
        override fun SemanticsPropertyReceiver.applySemantics() {
            maxTextLength = maximum
        }

        override fun TextFieldBuffer.transformInput() {
            limitUserEdit()
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun TextFieldBuffer.limitUserEdit() {
        val proposed = asCharSequence()
        // changes는 버퍼 수정 시 함께 바뀌므로 실제 편집 범위를 먼저 고정한다.
        val editedRanges = mutableListOf<TextRange>()
        repeat(changes.changeCount) { index ->
            val range = changes.getRange(index)
            if (!range.collapsed) {
                val start = codePointBoundary(proposed, range.min)
                val end = if (codePointBoundary(proposed, range.max) != range.max) range.max + 1 else range.max
                val previous = editedRanges.lastOrNull()
                if (previous != null && start <= previous.end) {
                    editedRanges[editedRanges.lastIndex] = TextRange(previous.start, maxOf(previous.end, end))
                } else {
                    editedRanges += TextRange(start, end)
                }
            }
        }

        fun nextEnd(index: Int, end: Int): Int =
            if (index + 1 < end && proposed[index].isHighSurrogate() && proposed[index + 1].isLowSurrogate()) index + 2 else index + 1
        fun valid(index: Int, end: Int): Boolean =
            (end - index == 2 || !proposed[index].isSurrogate()) && (!digitsOnly || proposed[index].isDigit())
        fun cost(start: Int, end: Int): Int = if (utf16) end - start else 1
        fun unchangedCost(start: Int, end: Int): Int {
            var index = start
            var count = 0
            while (index < end) {
                val next = nextEnd(index, end)
                if (valid(index, next)) count += cost(index, next)
                index = next
            }
            return count
        }

        var untouchedStart = 0
        var reserved = 0
        editedRanges.forEach { range ->
            reserved += unchangedCost(untouchedStart, range.start)
            untouchedStart = range.end
        }
        reserved += unchangedCost(untouchedStart, length)
        var remaining = (maximum - reserved).coerceAtLeast(0)
        val rejectedRanges = mutableListOf<TextRange>()
        fun reject(start: Int, end: Int) {
            if (start == end) return
            val previous = rejectedRanges.lastOrNull()
            if (previous != null && previous.end == start) {
                rejectedRanges[rejectedRanges.lastIndex] = TextRange(previous.start, end)
            } else {
                rejectedRanges += TextRange(start, end)
            }
        }
        fun filter(start: Int, end: Int, inserted: Boolean) {
            var index = start
            while (index < end) {
                // 허용량을 다 쓰면 나머지 붙여넣기를 복사하거나 순회하지 않는다.
                if (inserted && remaining == 0) {
                    reject(index, end)
                    break
                }
                val next = nextEnd(index, end)
                val units = cost(index, next)
                if (!valid(index, next)) {
                    reject(index, next)
                } else if (inserted && units > remaining) {
                    reject(index, end)
                    break
                } else if (inserted) {
                    remaining -= units
                }
                index = next
            }
        }
        untouchedStart = 0
        editedRanges.forEach { range ->
            filter(untouchedStart, range.start, inserted = false)
            filter(range.start, range.end, inserted = true)
            untouchedStart = range.end
        }
        filter(untouchedStart, length, inserted = false)

        val previousSelection = selection
        fun adjusted(offset: Int): Int = offset - rejectedRanges.sumOf { range ->
            (offset - range.start).coerceIn(0, range.length)
        }
        val acceptedSelection = TextRange(adjusted(previousSelection.start), adjusted(previousSelection.end))
        rejectedRanges.asReversed().forEach { replace(it.start, it.end, "") }
        val boundedSelection = safeSelection(asCharSequence(), acceptedSelection.start, acceptedSelection.end)
        if (selection != boundedSelection) selection = boundedSelection
    }

    // 이전 버전의 String 초안도 읽어 업데이트 후 복원에서 크기 제한을 우회하지 않는다.
    val saver = Saver<TextFieldState, Any>(
        save = { state ->
            val text = limit(state.text.toString())
            val selection = safeSelection(text, state.selection.start, state.selection.end)
            arrayListOf(text, selection.start, selection.end)
        },
        restore = { saved ->
            when (saved) {
                is String -> restoredState(saved, saved.length, saved.length)
                is List<*> -> restoredState(saved[0] as String, saved[1] as Int, saved[2] as Int)
                else -> null
            }
        },
    )

    fun restoredState(text: String, selectionStart: Int, selectionEnd: Int): TextFieldState {
        val accepted = limit(text)
        return TextFieldState(accepted, safeSelection(accepted, selectionStart, selectionEnd))
    }

    private fun safeSelection(text: CharSequence, start: Int, end: Int): TextRange =
        TextRange(codePointBoundary(text, start), codePointBoundary(text, end))

    private fun codePointBoundary(text: CharSequence, offset: Int): Int {
        val bounded = offset.coerceIn(0, text.length)
        return if (bounded > 0 && bounded < text.length && text[bounded].isLowSurrogate() && text[bounded - 1].isHighSurrogate()) bounded - 1 else bounded
    }
}

@Composable
internal fun rememberLimitedTextFieldState(
    policy: TextInputPolicy,
    initialText: String = "",
    vararg inputs: Any?,
): TextFieldState {
    val saver = remember(policy) { policy.saver }
    return rememberSaveable(*inputs, policy, saver = saver) {
        val accepted = policy.limit(initialText)
        TextFieldState(accepted, TextRange(accepted.length))
    }
}
