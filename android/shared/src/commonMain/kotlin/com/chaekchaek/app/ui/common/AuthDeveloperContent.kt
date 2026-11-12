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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("인증과 토큰 만료", style = MaterialTheme.typography.titleMedium)
        val accessState = if (debugInfo.hasAccessToken) {
            "유효 (남은 시간: ${debugInfo.accessTokenExpiresInSeconds ?: 0}초)"
        } else {
            "없음 / 만료됨"
        }
        val refreshState = if (debugInfo.hasRefreshToken) "있음" else "없음"
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
            onClick = {
                authViewModel.expireAccessTokenForTesting()
                statusMessage = "Access 토큰이 즉시 만료 상태로 치환되었습니다. 다음 API 요청 시 Silent Refresh가 동작합니다."
            },
            modifier = Modifier.fillMaxWidth(),
        )
        ChaekOverlayButton(
            label = "Refresh 토큰 서버 폐기 (로컬 유지)",
            onClick = {
                authViewModel.revokeRefreshTokenForTesting { result ->
                    statusMessage = if (result.isSuccess) {
                        "서버 측 Refresh 토큰이 폐기되었습니다. (로컬 세션 유지됨)"
                    } else {
                        "Refresh 토큰 폐기 요청 실패: ${result.exceptionOrNull()?.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
