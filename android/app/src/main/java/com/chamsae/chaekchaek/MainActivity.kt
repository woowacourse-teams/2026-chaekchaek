package com.chamsae.chaekchaek

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import com.chamsae.chaekchaek.auth.RefreshTokenStore
import com.chamsae.chaekchaek.auth.requestGoogleIdToken
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val density = LocalDensity.current
            val usesThreeButtonNavigation =
                WindowInsets.mandatorySystemGestures.getBottom(density) <=
                        WindowInsets.navigationBars.getBottom(density)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars =
                    usesThreeButtonNavigation
            }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val tokenStore = remember(context) { RefreshTokenStore(context) }
            val authPlatform = remember(context, scope, tokenStore) {
                AuthPlatformCallbacks(
                    requestGoogleIdToken = { onResult ->
                        scope.launch {
                            runCatching { requestGoogleIdToken(context) }
                                .onSuccess { onResult(it, null) }
                                .onFailure { onResult(null, "Google 로그인을 완료하지 못했어요.") }
                        }
                    },
                    readRefreshToken = tokenStore::read,
                    writeRefreshToken = tokenStore::write,
                    clearRefreshToken = tokenStore::clear,
                    readGuest = tokenStore::readGuest,
                    writeGuest = tokenStore::writeGuest,
                    clearGuest = tokenStore::clearGuest,
                )
            }
            App(authPlatform)
        }
    }
}
