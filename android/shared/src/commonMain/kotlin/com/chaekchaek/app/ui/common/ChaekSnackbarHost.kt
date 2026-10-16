package com.chaekchaek.app.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 부모에서 소비한 시스템 여백은 중복 적용하지 않는다. 고정 UI 높이는 시스템 여백을 제외한다.
@Composable
internal fun BoxScope.ChaekSnackbarHost(
    hostState: SnackbarHostState,
    bottomContentHeight: Dp = 0.dp,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = Modifier.align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = bottomContentHeight),
    )
}
