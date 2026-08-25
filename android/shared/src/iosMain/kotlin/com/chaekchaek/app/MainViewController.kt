package com.chaekchaek.app

import androidx.compose.ui.window.ComposeUIViewController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import platform.UIKit.UIViewController

fun MainViewController(authPlatform: AuthPlatformCallbacks): UIViewController =
  ComposeUIViewController { App(authPlatform) }
