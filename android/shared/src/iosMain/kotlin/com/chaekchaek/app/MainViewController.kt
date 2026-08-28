package com.chaekchaek.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.common.LocalGoogleSignInButtonFactory
import platform.UIKit.UIControl
import platform.UIKit.UIViewController

fun MainViewController(
  authPlatform: AuthPlatformCallbacks,
  createGoogleSignInButton: () -> UIControl,
): UIViewController {
  return ComposeUIViewController {
    CompositionLocalProvider(LocalGoogleSignInButtonFactory provides createGoogleSignInButton) {
      App(authPlatform)
    }
  }
}
