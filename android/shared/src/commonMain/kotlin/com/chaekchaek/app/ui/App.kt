package com.chaekchaek.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.LocalAnalyticsTracker
import com.chaekchaek.app.ui.theme.ChaekchaekTheme

@Composable
fun App(
    authPlatform: AuthPlatformCallbacks,
    analytics: AnalyticsTracker = AnalyticsTracker.None,
    uiTestingMyPage: Boolean = false,
) {
    ChaekchaekTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalAnalyticsTracker provides analytics) {
                AppNavigation(authPlatform, analytics, uiTestingMyPage)
            }
        }
    }
}
