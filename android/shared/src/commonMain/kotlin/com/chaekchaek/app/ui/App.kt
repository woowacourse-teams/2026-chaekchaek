package com.chaekchaek.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.theme.ChaekchaekTheme

@Composable
fun App(authPlatform: AuthPlatformCallbacks, uiTestingMyPage: Boolean = false) {
    ChaekchaekTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppNavigation(authPlatform, uiTestingMyPage)
        }
    }
}
