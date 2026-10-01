package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

private fun AppError.message(): String =
    when (this) {
        AppError.Network -> "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        AppError.NotFound -> "홈 피드를 찾을 수 없어요."
        AppError.Unauthorized -> "로그인이 필요한 요청이에요."
        AppError.Unknown -> "잠시 후 다시 시도해 주세요."
    }
