package com.chaekchaek.app.ui.input

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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

    val inputTransformation: InputTransformation = InputTransformation {
        val proposed = asCharSequence().toString()
        val accepted = applyChange(originalText.toString(), proposed)
        if (accepted != proposed) {
            val previousSelection = selection
            // 기존 접미부는 유지하고 삭제된 삽입분 뒤의 커서만 앞으로 당긴다.
            var prefix = 0
            while (prefix < accepted.length && prefix < proposed.length && accepted[prefix] == proposed[prefix]) prefix++
            val removed = proposed.length - accepted.length
            fun adjusted(offset: Int): Int =
                (if (offset <= prefix) offset else (offset - removed).coerceAtLeast(prefix))
                    .coerceIn(0, accepted.length)
            replace(0, length, accepted)
            selection = safeSelection(accepted, adjusted(previousSelection.start), adjusted(previousSelection.end))
        }
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

    private fun safeSelection(text: String, start: Int, end: Int): TextRange {
        fun boundary(offset: Int): Int {
            val bounded = offset.coerceIn(0, text.length)
            return if (bounded > 0 && bounded < text.length && text[bounded].isLowSurrogate() && text[bounded - 1].isHighSurrogate()) bounded - 1 else bounded
        }
        return TextRange(boundary(start), boundary(end))
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
