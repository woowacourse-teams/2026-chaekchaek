package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.presentation.common.AppError

@Composable
internal fun HomeLoadErrorContent(
    error: AppError,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize())
    ChaekOneActionDialog(
        title = { Text("홈을 불러오지 못했어요") },
        text = { Text(error.message()) },
        confirmButton = { TextButton(onClick = retry) { Text("다시 시도") } },
    )
}

@Composable
internal fun HomeLoadErrorPanel(
    error: AppError,
    retry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "홈을 불러오지 못했어요",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                error.message(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        ChaekOverlayButton(
            label = "다시 시도",
            onClick = retry,
            modifier = Modifier.padding(top = 24.dp).width(120.dp),
        )
    }
}

private fun AppError.message(): String =
    when (this) {
        AppError.Network -> "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        AppError.NotFound -> "홈 피드를 찾을 수 없어요."
        AppError.Unauthorized -> "로그인이 필요한 요청이에요."
        AppError.Unknown -> "잠시 후 다시 시도해 주세요."
    }
