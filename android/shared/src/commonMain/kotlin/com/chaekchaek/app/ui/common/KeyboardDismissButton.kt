package com.chaekchaek.app.ui.common

import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.ui.theme.ChaekInk
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chaekchaek.app.ui.input.rememberKeyboardDismissal

@Composable
internal fun KeyboardDismissButton(modifier: Modifier = Modifier) {
    val dismissKeyboard = rememberKeyboardDismissal()
    TextButton(onClick = dismissKeyboard, modifier = modifier.heightIn(min = 36.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text("키보드 닫기", fontSize = 12.sp, color = ChaekInk, textDecoration = TextDecoration.Underline)
    }
}
