package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

@Composable
internal fun ChaekOneActionDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) = ChaekAlertDialog(onDismissRequest, confirmButton, title = title, text = text)

@Composable
internal fun ChaekTwoActionDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) = ChaekAlertDialog(onDismissRequest, confirmButton, dismissButton, title, text)

@Composable
private fun ChaekAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 358.dp).fillMaxWidth(),
            shape = ChaekOverlayTokens.containerShape,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(ChaekOverlayTokens.contentPadding),
                verticalArrangement = Arrangement.spacedBy(ChaekOverlayTokens.sectionSpacing),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
                            title?.invoke()
                        }
                    }
                    ChaekCloseButton(onDismissRequest)
                }
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) { text?.invoke() }
                Box(Modifier.fillMaxWidth().heightIn(min = ChaekOverlayTokens.buttonHeight), contentAlignment = Alignment.Center) { confirmButton() }
                dismissButton?.let {
                    Box(Modifier.fillMaxWidth().heightIn(min = ChaekOverlayTokens.buttonHeight), contentAlignment = Alignment.Center) { it() }
                }
            }
        }
    }
}

@Composable
internal fun ChaekCloseButton(onClick: () -> Unit) {
    IconButton(onClick, modifier = Modifier.size(44.dp).semantics { contentDescription = "닫기" }) {
        Text("×", fontFamily = ChaekIconFontFamily(), style = MaterialTheme.typography.titleMedium)
    }
}
