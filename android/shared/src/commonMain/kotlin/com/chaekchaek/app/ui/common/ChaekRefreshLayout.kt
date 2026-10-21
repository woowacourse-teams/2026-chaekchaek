package com.chaekchaek.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.chaekchaek.app.presentation.common.RefreshUiState
import com.chaekchaek.app.ui.theme.ChaekActionSurface
import com.chaekchaek.app.ui.theme.ChaekOnActionSurface
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private const val RefreshMinimumVisibleMillis = 300L

/** Figma PullRefresh 1184:428 / C 1447:5952. 헤더와 고정 바는 이 컨테이너 밖에 둔다. */
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
    val pullState = rememberPullToRefreshState()
    val bottomLimit = with(LocalDensity.current) { 24.dp.toPx() }
    var bottomOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var minimumFeedbackVisible by remember { mutableStateOf(false) }
    var feedbackTimer by remember { mutableStateOf<Job?>(null) }
    val refreshingVisible = state.isRefreshing || minimumFeedbackVisible
    var settling by remember { androidx.compose.runtime.mutableStateOf<Job?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val latestContent = rememberUpdatedState(content)
    // 편집 모드에서 공식 Box를 제거해도 목록과 입력의 remember 상태를 유지한다.
    val retainedContent = remember { movableContentOf { latestContent.value() } }

    fun holdRefreshFeedback() {
        minimumFeedbackVisible = true
        feedbackTimer?.cancel()
        feedbackTimer = scope.launch {
            delay(RefreshMinimumVisibleMillis)
            minimumFeedbackVisible = false
        }
    }

    fun requestRefresh(afterFailure: Boolean = false) {
        if (!latestEnabled.value || latestState.value.isRefreshing || (minimumFeedbackVisible && !afterFailure)) return
        // 응답이 같은 프레임 안에 끝나도 사용자 동작의 피드백은 남긴다.
        holdRefreshFeedback()
        latestRefresh.value()
    }

    LaunchedEffect(state.isRefreshing, canPull) {
        if (!canPull) {
            feedbackTimer?.cancel()
            minimumFeedbackVisible = false
        } else if (state.isRefreshing && !minimumFeedbackVisible) {
            holdRefreshFeedback()
        }
    }

    fun settleBottom() {
        settling?.cancel()
        settling = scope.launch {
            animate(bottomOffset, 0f, animationSpec = tween(220, easing = LinearOutSlowInEasing)) { value, _ ->
                bottomOffset = value
            }
        }
    }

    LaunchedEffect(state.isRefreshing, canPull, bottomEnabled) {
        if (state.isRefreshing || !canPull || !bottomEnabled) settleBottom()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            if (snackbar.showSnackbar(it, actionLabel = "다시 시도") == SnackbarResult.ActionPerformed &&
                latestEnabled.value && !latestState.value.isRefreshing) requestRefresh(afterFailure = true)
        }
    }
    val bottomConnection = remember(bottomLimit, pullState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !latestEnabled.value || latestState.value.isRefreshing) return Offset.Zero
                if (bottomOffset < 0f && available.y > 0f) {
                    settling?.cancel()
                    val consumed = available.y.coerceAtMost(-bottomOffset / 0.5f)
                    bottomOffset += consumed * 0.5f
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || !latestEnabled.value || latestState.value.isRefreshing) return Offset.Zero
                // 아래로 당기는 양은 소비하지 않아 공식 상단 제스처에 전달한다.
                if (available.y < 0f && latestBottom.value && pullState.distanceFraction == 0f) {
                    settling?.cancel()
                    bottomOffset = (bottomOffset + available.y * 0.5f).coerceIn(-bottomLimit, 0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (bottomOffset == 0f) return Velocity.Zero
                settleBottom()
                return Velocity(0f, available.y)
            }
        }
    }
    val bodyModifier = Modifier.fillMaxSize().nestedScroll(bottomConnection)
        .graphicsLayer { translationY = bottomOffset }
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        Box(modifier.fillMaxSize().clipToBounds().semantics {
            if (canPull && !refreshingVisible) {
                customActions = listOf(CustomAccessibilityAction("새로고침") { requestRefresh(); true })
            }
        }) {
            if (canPull) {
                PullToRefreshBox(
                    isRefreshing = refreshingVisible,
                    onRefresh = { requestRefresh() },
                    modifier = Modifier.fillMaxSize(),
                    state = pullState,
                    indicator = {
                        if (refreshingVisible || pullState.distanceFraction > 0f) {
                            RefreshIndicator(
                                refreshing = refreshingVisible,
                                ready = pullState.distanceFraction >= 1f,
                                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().heightIn(min = 64.dp).graphicsLayer {
                                    translationY = if (refreshingVisible) 0f else
                                        (pullState.distanceFraction.coerceAtMost(1f) - 1f) * size.height
                                },
                            )
                        }
                    },
                ) {
                    Box(bodyModifier) { retainedContent() }
                }
            } else {
                // Box에는 enabled 인수가 없어 편집 중에는 제스처 노드 자체를 분리한다.
                Box(bodyModifier) { retainedContent() }
            }
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
private fun RefreshIndicator(refreshing: Boolean, ready: Boolean, modifier: Modifier) {
    val label = when {
        refreshing -> "새로고침 중이에요"
        ready -> "놓으면 새로고침"
        else -> "당겨서 새로고침"
    }
    Box(modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.background(ChaekActionSurface, RoundedCornerShape(18.dp))
                .heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("pull-refresh-indicator")
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (refreshing) RefreshSpinner(Modifier.testTag("pull-refresh-spinner"))
            Text(
                label,
                modifier = Modifier.weight(1f, fill = false),
                color = ChaekOnActionSurface,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp, lineHeight = 19.5.sp, letterSpacing = (-0.426).sp,
                ),
            )
        }
    }
}

@Composable
private fun RefreshSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart))
    Image(painterResource(Res.drawable.ic_refresh_loader), contentDescription = null,
        modifier = modifier.size(20.dp).graphicsLayer { rotationZ = rotation })
}

@Composable
fun ListBoundary(
    hasNextPage: Boolean = false,
    loading: Boolean = false,
    showLoading: Boolean = false,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val label = when {
        failed -> "불러오지 못했어요"
        loading && showLoading -> "더 불러오는 중이에요"
        hasNextPage || loading -> "다음 내용을 요청하고 있어요"
        else -> return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp)
            .testTag("list-boundary").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading && showLoading) RefreshSpinner()
        Text(
            label,
            modifier = Modifier.weight(1f, fill = false),
            color = ChaekInkSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
        )
        if (failed) TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 44.dp)) { Text("다시 시도") }
    }
}
