package com.chaekchaek.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.bookdetail.BookQuestionAPreviewScreen
import com.chaekchaek.app.ui.common.LocalGoogleSignInButtonFactory
import com.chaekchaek.app.ui.search.RecentSearchStorage
import com.chaekchaek.app.ui.onboarding.OnboardingStorage
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import platform.UIKit.UIControl
import platform.UIKit.UIViewController

fun MainViewController(
  authPlatform: AuthPlatformCallbacks,
  recentSearchStorage: RecentSearchStorage,
  onboardingStorage: OnboardingStorage,
  createGoogleSignInButton: () -> UIControl,
  analyticsEnvironment: String = "production",
  logAnalyticsEvent: (AnalyticsEvent) -> Unit = {},
  uiTestingMyPage: Boolean = false,
  uiTestingBookQuestionA: Boolean = false,
  showQuestionAPreviewControls: Boolean = true,
): UIViewController {
  val analytics = AnalyticsTracker(analyticsEnvironment, logAnalyticsEvent)
  return ComposeUIViewController {
    CompositionLocalProvider(LocalGoogleSignInButtonFactory provides createGoogleSignInButton) {
      if (uiTestingBookQuestionA) {
        ChaekchaekTheme {
          Box(Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
          )) {
            BookQuestionAPreviewScreen(showControls = showQuestionAPreviewControls)
          }
        }
      } else App(
        authPlatform = authPlatform,
        recentSearchStorage = recentSearchStorage,
        onboardingStorage = onboardingStorage,
        analytics = analytics,
        uiTestingMyPage = uiTestingMyPage,
      )
    }
  }
}
