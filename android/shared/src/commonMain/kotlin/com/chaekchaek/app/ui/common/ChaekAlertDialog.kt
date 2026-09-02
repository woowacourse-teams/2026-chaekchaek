package com.chaekchaek.app.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

private val ChaekDialogShape = RoundedCornerShape(12.dp)

@Composable
internal fun ChaekOneActionDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    ChaekAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        title = title,
        text = text,
    )
}

@Composable
internal fun ChaekTwoActionDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    ChaekAlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
    )
}

@Composable
private fun ChaekAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.shadow(12.dp, ChaekDialogShape),
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = ChaekDialogShape,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
