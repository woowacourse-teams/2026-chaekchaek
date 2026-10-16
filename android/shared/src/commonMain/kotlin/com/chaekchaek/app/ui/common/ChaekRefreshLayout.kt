package com.chaekchaek.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_refresh_loader
import chaekchaek.shared.generated.resources.ic_refresh_pull
import chaekchaek.shared.generated.resources.ic_refresh_ready
import com.chaekchaek.app.presentation.common.RefreshUiState
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** Figma PullRefresh 1184:428 / ListBoundary 1184:441. 헤더와 고정 바는 이 컨테이너 밖에 둔다. */
@Composable
fun ChaekRefreshLayout(
    state: RefreshUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bottomEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val latestState = rememberUpdatedState(state)
    val latestRefresh = rememberUpdatedState(onRefresh)
    val canPull = enabled && state.enabled
    val latestEnabled = rememberUpdatedState(canPull)
    val latestBottom = rememberUpdatedState(bottomEnabled)
    val density = LocalDensity.current
    val threshold = with(density) { 64.dp.toPx() }
    val bottomLimit = with(density) { 24.dp.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var settling by remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    val snackbar = remember { SnackbarHostState() }

    fun settle(target: Float) {
        settling?.cancel()
        settling = scope.launch {
            animate(offset, target, animationSpec = tween(220, easing = LinearOutSlowInEasing)) { value, _ -> offset = value }
        }
    }

    LaunchedEffect(state.isRefreshing, canPull) {
        if (canPull && state.isRefreshing) {
            settling?.cancel()
            offset = threshold
        } else settle(0f)
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            if (snackbar.showSnackbar(it, actionLabel = "다시 시도") == SnackbarResult.ActionPerformed &&
                latestEnabled.value && !latestState.value.isRefreshing) latestRefresh.value()
        }
    }
    val connection = remember(threshold, bottomLimit) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !latestEnabled.value || latestState.value.isRefreshing) return Offset.Zero
                if ((offset > 0 && available.y < 0) || (offset < 0 && available.y > 0)) {
                    settling?.cancel()
                    val consumed = if (offset > 0) available.y.coerceAtLeast(-offset / 0.5f)
                        else available.y.coerceAtMost(-offset / 0.5f)
                    offset += consumed * 0.5f
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !latestEnabled.value || latestState.value.isRefreshing) return Offset.Zero
                if (available.y > 0 || (available.y < 0 && latestBottom.value)) {
                    settling?.cancel()
                    offset = (offset + available.y * 0.5f).coerceIn(-bottomLimit, threshold)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset == 0f || latestState.value.isRefreshing) return Velocity.Zero
                val refreshing = latestEnabled.value && offset >= threshold - 0.5f
                if (refreshing) latestRefresh.value()
                settling?.cancel()
                settling = scope.launch {
                    // 빠른 응답이 같은 프레임에서 끝나도 표시가 열린 채 남지 않는다.
                    withFrameNanos { }
                    if (latestState.value.isRefreshing && latestEnabled.value) offset = threshold
                    else settle(0f)
                }
                return Velocity(0f, available.y)
            }
        }
    }
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Box(modifier.fillMaxSize().clipToBounds().nestedScroll(connection).semantics {
            if (canPull && !state.isRefreshing) {
                customActions = listOf(CustomAccessibilityAction("새로고침") { onRefresh(); true })
            }
        }) {
            if (offset > 0f) {
                RefreshIndicator(
                    state = state,
                    ready = offset >= threshold - 0.5f,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                        .graphicsLayer { translationY = offset - threshold },
                )
            }
            Box(Modifier.fillMaxSize().graphicsLayer { translationY = offset }) { content() }
            ChaekSnackbarHost(snackbar)
        }
    }
}

/** 정적 빈 화면에도 세로 스크롤 경계를 제공하고 내용의 기존 중앙 정렬을 유지한다. */
@Composable
fun RefreshableEmptyContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

@Composable
private fun RefreshIndicator(state: RefreshUiState, ready: Boolean, modifier: Modifier) {
    val label = when {
        state.showLoading -> "새로고침 중이에요"
        state.isRefreshing -> "갱신 요청 중"
        ready -> "놓으면 새로고침"
        else -> "당겨서 새로고침"
    }
    Row(
        modifier.testTag("pull-refresh-indicator").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.showLoading) RefreshSpinner()
        else Image(
            painterResource(if (ready || state.isRefreshing) Res.drawable.ic_refresh_ready else Res.drawable.ic_refresh_pull),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Text(label, color = if (ready && !state.isRefreshing) ChaekInk else ChaekInkSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.5.sp))
    }
}

@Composable
private fun RefreshSpinner() {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart))
    Image(painterResource(Res.drawable.ic_refresh_loader), contentDescription = null,
        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation })
}

@Composable
fun ListBoundary(
    hasNextPage: Boolean = false,
    loading: Boolean = false,
    showLoading: Boolean = false,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp)
            .testTag("list-boundary").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading && showLoading) RefreshSpinner()
        Text(
            when {
                failed -> "불러오지 못했어요"
                loading && showLoading -> "더 불러오는 중이에요"
                hasNextPage -> "다음 내용을 요청하고 있어요"
                else -> "여기까지 모두 봤어요"
            },
            modifier = Modifier.weight(1f, fill = false),
            color = ChaekInkSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
        )
        if (failed) TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 44.dp)) { Text("다시 시도") }
    }
}
