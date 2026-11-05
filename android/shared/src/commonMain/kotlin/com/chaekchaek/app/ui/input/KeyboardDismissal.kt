package com.chaekchaek.app.ui.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
internal fun rememberKeyboardDismissal(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController) {
        {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}

private class KeyboardTapGuard {
    var startedInInput = false
}

private val LocalKeyboardTapGuard = staticCompositionLocalOf<KeyboardTapGuard?> { null }

@Composable
internal fun KeyboardDismissalArea(
    enabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    val guard = remember { KeyboardTapGuard() }
    val dismissKeyboard = rememberKeyboardDismissal()
    CompositionLocalProvider(LocalKeyboardTapGuard provides if (enabled) guard else null) {
        content(if (enabled) Modifier.pointerInput(guard, dismissKeyboard) {
            awaitEachGesture {
                // Initial travels parent to child. Reset before input descendants mark this tap.
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                guard.startedInInput = false
                val downEvent = awaitPointerEvent(pass = PointerEventPass.Final)
                if (!guard.startedInInput && downEvent.changes.none { it.isConsumed }) {
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                    if (up != null && !up.isConsumed) dismissKeyboard()
                }
            }
        } else Modifier)
    }
}

// Mark the hit path without consuming the event: field editing and selection remain in charge.
internal fun Modifier.excludeFromKeyboardDismissal(): Modifier = composed {
    val guard = LocalKeyboardTapGuard.current
    if (guard == null) this else pointerInput(guard) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            guard.startedInInput = true
        }
    }
}
