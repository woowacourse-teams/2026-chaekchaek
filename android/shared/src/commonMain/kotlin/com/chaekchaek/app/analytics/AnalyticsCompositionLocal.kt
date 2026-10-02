package com.chaekchaek.app.analytics

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAnalyticsTracker = staticCompositionLocalOf { AnalyticsTracker.None }
