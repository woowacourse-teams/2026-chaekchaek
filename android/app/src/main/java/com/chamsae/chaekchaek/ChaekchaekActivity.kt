package com.chamsae.chaekchaek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.chamsae.chaekchaek.auth.RefreshTokenStore
import com.chamsae.chaekchaek.auth.requestGoogleIdToken
import com.chamsae.chaekchaek.search.RecentSearchPreferences
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.search.RecentSearchStorage
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch

abstract class ChaekchaekActivity : ComponentActivity() {
    protected abstract val analyticsEnvironment: String

    @Composable
    protected open fun HomeHeaderAction() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val tokenStore = remember(context) { RefreshTokenStore(context) }
            val recentSearchPreferences = remember(context) { RecentSearchPreferences(context) }
            val analytics = remember(context, analyticsEnvironment) {
                val logger = FirebaseAnalyticsEventLogger(FirebaseAnalytics.getInstance(context))
                AnalyticsTracker(
                    environment = analyticsEnvironment,
                    sink = logger::log,
                )
            }
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
            App(
                authPlatform = authPlatform,
                analytics = analytics,
                homeHeaderAction = { HomeHeaderAction() },
                onExitRequested = { finish() },
                recentSearchStorage = RecentSearchStorage(
                    read = recentSearchPreferences::read,
                    write = recentSearchPreferences::write,
                ),
            )
        }
    }
}
