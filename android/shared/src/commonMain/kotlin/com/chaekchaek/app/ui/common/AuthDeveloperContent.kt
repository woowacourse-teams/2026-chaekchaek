package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.auth.AuthViewModel

@Composable
fun AuthDeveloperContent(authViewModel: AuthViewModel) {
    val debugInfo by authViewModel.tokenDebugInfo.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var actionInProgress by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("인증과 토큰 만료", style = MaterialTheme.typography.titleMedium)
        val accessState = tokenState(debugInfo.hasAccessToken, debugInfo.accessTokenExpiresInSeconds)
        val refreshState = tokenState(debugInfo.hasRefreshToken, debugInfo.refreshTokenExpiresInSeconds)
        Text(
            "Access 토큰: $accessState\nRefresh 토큰: $refreshState",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (statusMessage != null) {
            Text(
                statusMessage ?: "",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        ChaekOverlayButton(
            label = "Access 토큰 강제 만료",
            enabled = debugInfo.hasAccessToken && !actionInProgress,
            onClick = {
                actionInProgress = true
                authViewModel.expireAccessTokenForTesting { result ->
                    actionInProgress = false
                    statusMessage = if (result.isSuccess) "Access 토큰을 강제 만료했습니다." else "Access 토큰을 만료하지 못했습니다."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        ChaekOverlayButton(
            label = "Refresh 토큰 서버 폐기 (로컬 유지)",
            enabled = debugInfo.hasRefreshToken && !actionInProgress,
            onClick = {
                actionInProgress = true
                authViewModel.revokeRefreshTokenForTesting { result ->
                    actionInProgress = false
                    statusMessage = if (result.isSuccess) {
                        "서버 측 Refresh 토큰이 폐기되었습니다. (로컬 세션 유지됨)"
                    } else {
                        "Refresh 토큰 폐기 요청에 실패했습니다."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun tokenState(present: Boolean, secondsRemaining: Long?): String = when {
    !present -> "없음"
    secondsRemaining == null -> "만료 시간 확인 중"
    secondsRemaining == 0L -> "만료 또는 폐기됨"
    else -> "남은 시간 ${secondsRemaining}초"
}
