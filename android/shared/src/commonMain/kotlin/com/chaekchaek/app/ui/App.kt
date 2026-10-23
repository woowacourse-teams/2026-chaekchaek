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
import com.chaekchaek.app.ui.search.RecentSearchStorage
import com.chaekchaek.app.ui.onboarding.OnboardingStorage

@Composable
fun App(
    authPlatform: AuthPlatformCallbacks,
    recentSearchStorage: RecentSearchStorage = RecentSearchStorage(),
    onboardingStorage: OnboardingStorage = OnboardingStorage(),
    analytics: AnalyticsTracker = AnalyticsTracker.None,
    uiTestingMyPage: Boolean = false,
    refreshAnalyticsConfiguration: () -> Unit = {},
    homeHeaderAction: @Composable () -> Unit = {},
    onExitRequested: () -> Unit = {},
    policyController: com.chaekchaek.app.config.MobilePolicyController? = null,
    onUpdate: () -> Unit = {},
) {
    com.chaekchaek.app.analytics.AnalyticsLifecycleEffect(analytics, refreshAnalyticsConfiguration)
    ChaekchaekTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            androidx.compose.runtime.CompositionLocalProvider(LocalAnalyticsTracker provides analytics) {
                AppNavigation(
                    authPlatform = authPlatform,
                    recentSearchStorage = recentSearchStorage,
                    onboardingStorage = onboardingStorage,
                    analytics = analytics,
                    uiTestingMyPage = uiTestingMyPage,
                    homeHeaderAction = homeHeaderAction,
                    onExitRequested = onExitRequested,
                )
                policyController?.let {
                    com.chaekchaek.app.ui.common.UpdatePolicyHost(it, onUpdate)
                    com.chaekchaek.app.ui.common.ErrorRecoveryEffect(it.errors)
                }
            }
        }
    }
}
