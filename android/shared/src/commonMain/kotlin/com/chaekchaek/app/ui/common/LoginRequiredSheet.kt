package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        containerColor = ChaekSurface,
        shape = ChaekOverlayTokens.sheetShape,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(ChaekOverlayTokens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(ChaekOverlayTokens.sectionSpacing),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("로그인이 필요해요", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                ChaekCloseButton(onDismiss)
            }
            Text(
                "내 독서 기록을 남기고 감상에 참여하려면 로그인해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
            )
            error?.let {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = ChaekOverlayTokens.cardShape) {
                    Text(it, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (appleSignInAvailable) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AppleSignInButton(
                        signingIn = signingIn,
                        onClick = onAppleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GoogleSignInButton(
                        signingIn = signingIn,
                        onClick = onGoogleSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Surface(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = !signingIn,
                    shape = RoundedCornerShape(8.dp),
                    color = ChaekInk,
                ) {
                    Text(
                        if (signingIn) "로그인 중..." else "Google로 계속하기",
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = ChaekSurface,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            ChaekOverlayButton("닫기", onDismiss, secondary = true)
        }
    }
}
