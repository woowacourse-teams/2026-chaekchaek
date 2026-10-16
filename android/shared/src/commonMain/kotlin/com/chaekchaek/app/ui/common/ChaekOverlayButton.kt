package com.chaekchaek.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens

@Composable
internal fun ChaekOverlayButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    secondary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = ChaekOverlayTokens.buttonHeight),
        shape = ChaekOverlayTokens.buttonShape,
        color = if (!enabled) colors.surfaceVariant else if (secondary) colors.surface else colors.onBackground,
        border = if (secondary) BorderStroke(1.dp, colors.outlineVariant) else null,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (!enabled) colors.onSurfaceVariant else if (secondary) colors.onBackground else colors.surface,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}
