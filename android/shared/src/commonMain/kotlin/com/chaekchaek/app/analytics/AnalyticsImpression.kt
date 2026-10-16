package com.chaekchaek.app.analytics

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay

fun Modifier.analyticsImpression(
    contentType: String,
    contentId: String,
    bookKey: String?,
    listId: String,
    position: Int,
    isOwn: Boolean? = null,
): Modifier = composed {
    val analytics = LocalAnalyticsTracker.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var atLeastHalfVisible by remember(contentType, contentId, listId) { mutableStateOf(false) }
    val latestVisible by rememberUpdatedState(atLeastHalfVisible)
    val latestLifecycleState by rememberUpdatedState(lifecycleState)

    LaunchedEffect(atLeastHalfVisible, lifecycleState, contentType, contentId, listId) {
        if (!atLeastHalfVisible || !lifecycleState.isAtLeast(Lifecycle.State.STARTED)) return@LaunchedEffect
        delay(IMPRESSION_DURATION_MILLIS)
        if (latestVisible && latestLifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
            analytics.contentImpression(contentType, contentId, bookKey, listId, position, isOwn)
        }
    }

    onGloballyPositioned { coordinates ->
        val fullArea = coordinates.size.width.toLong() * coordinates.size.height.toLong()
        val visibleBounds = coordinates.boundsInWindow(clipBounds = true)
        val visibleArea = visibleBounds.width * visibleBounds.height
        atLeastHalfVisible = fullArea > 0L && visibleArea / fullArea.toFloat() >= MIN_VISIBLE_FRACTION
    }
}

private const val IMPRESSION_DURATION_MILLIS = 1_000L
private const val MIN_VISIBLE_FRACTION = 0.5f
