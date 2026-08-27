package com.chaekchaek.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekSurface

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun LoginRequiredSheet(
    signingIn: Boolean,
    error: String?,
    appleSignInAvailable: Boolean,
    onDismiss: () -> Unit,
    onAppleSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("로그인이 필요해요", style = MaterialTheme.typography.titleLarge)
            Text(
                "내 독서 기록을 남기고 감상에 참여하려면 로그인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (appleSignInAvailable) {
                Surface(
                    onClick = { if (!signingIn) onAppleSignIn() },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = ChaekInk,
                ) {
                    Text(
                        if (signingIn) "로그인 중..." else "Apple로 계속하기",
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = ChaekSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Surface(
                onClick = { if (!signingIn) onGoogleSignIn() },
                modifier = Modifier.fillMaxWidth().padding(top = if (appleSignInAvailable) 0.dp else 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (appleSignInAvailable) ChaekSurface else ChaekInk,
                border = if (appleSignInAvailable) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            ) {
                Text(
                    if (signingIn) "로그인 중..." else "Google로 계속하기",
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = if (appleSignInAvailable) ChaekInk else ChaekSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
