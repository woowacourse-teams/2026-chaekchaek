package com.chaekchaek.app.ui.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

// Observe only taps that children did not handle, preserving field editing and scrolling.
internal fun Modifier.dismissKeyboardOnUnconsumedTap(onDismiss: () -> Unit): Modifier =
    pointerInput(onDismiss) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
            if (!down.isConsumed) {
                val up = waitForUpOrCancellation(pass = PointerEventPass.Final)
                if (up != null && !up.isConsumed) onDismiss()
            }
        }
    }
