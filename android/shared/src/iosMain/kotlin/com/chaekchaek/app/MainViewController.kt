package com.chaekchaek.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.common.LocalGoogleSignInButtonFactory
import com.chaekchaek.app.ui.search.RecentSearchStorage
import platform.UIKit.UIControl
import platform.UIKit.UIViewController

fun MainViewController(
  authPlatform: AuthPlatformCallbacks,
  recentSearchStorage: RecentSearchStorage,
  createGoogleSignInButton: () -> UIControl,
  uiTestingMyPage: Boolean = false,
): UIViewController {
  return ComposeUIViewController {
    CompositionLocalProvider(LocalGoogleSignInButtonFactory provides createGoogleSignInButton) {
      App(authPlatform, recentSearchStorage, uiTestingMyPage)
    }
  }
}
