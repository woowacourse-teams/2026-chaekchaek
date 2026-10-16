package com.chaekchaek.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.common.LocalGoogleSignInButtonFactory
import com.chaekchaek.app.ui.search.RecentSearchStorage
import platform.UIKit.UIControl
import platform.UIKit.UIViewController

fun MainViewController(
  authPlatform: AuthPlatformCallbacks,
  recentSearchStorage: RecentSearchStorage,
  createGoogleSignInButton: () -> UIControl,
  analyticsEnvironment: String = "production",
  logAnalyticsEvent: (AnalyticsEvent) -> Unit = {},
  uiTestingMyPage: Boolean = false,
): UIViewController {
  val analytics = AnalyticsTracker(analyticsEnvironment, logAnalyticsEvent)
  return ComposeUIViewController {
    CompositionLocalProvider(LocalGoogleSignInButtonFactory provides createGoogleSignInButton) {
      App(
        authPlatform = authPlatform,
        recentSearchStorage = recentSearchStorage,
        analytics = analytics,
        uiTestingMyPage = uiTestingMyPage,
      )
    }
  }
}
