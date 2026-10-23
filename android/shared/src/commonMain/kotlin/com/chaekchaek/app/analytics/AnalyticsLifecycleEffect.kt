package com.chaekchaek.app.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay

@Composable
fun AnalyticsLifecycleEffect(analytics: AnalyticsTracker, refreshConfiguration: () -> Unit) {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val refresh by rememberUpdatedState(refreshConfiguration)
    val foreground = state.isAtLeast(Lifecycle.State.RESUMED)
    LaunchedEffect(foreground, analytics) {
        analytics.setForeground(foreground)
        if (foreground) {
            refresh()
            var ticks = 0
            while (true) {
                delay(30_000)
                analytics.recordViewEngagement()
                if (++ticks % 2 == 0) refresh()
            }
        }
    }
    DisposableEffect(analytics) { onDispose { analytics.setForeground(false) } }
}
