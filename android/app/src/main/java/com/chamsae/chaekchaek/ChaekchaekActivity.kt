package com.chamsae.chaekchaek

import android.os.Bundle
import androidx.credentials.exceptions.GetCredentialCancellationException
import kotlinx.coroutines.CancellationException
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
import com.chaekchaek.app.analytics.AnalyticsEventDispatcher
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.AnalyticsCollectionController
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.search.RecentSearchStorage
import com.chaekchaek.app.ui.onboarding.OnboardingStorage
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.launch

abstract class ChaekchaekActivity : ComponentActivity() {
    protected abstract val analyticsEnvironment: String

    @Composable
    protected open fun HomeHeaderAction() = Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PostHogAnalyticsEventLogger.configure(applicationContext)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val tokenStore = remember(context) { RefreshTokenStore(context) }
            val recentSearchPreferences = remember(context) { RecentSearchPreferences(context) }
            val onboardingPreferences = remember(context) {
                context.getSharedPreferences("onboarding", MODE_PRIVATE).also { preferences ->
                    if (!preferences.contains("first_library_shown")) {
                        val info = context.packageManager.getPackageInfo(context.packageName, 0)
                        if (info.firstInstallTime != info.lastUpdateTime) {
                            preferences.edit().putBoolean("first_library_shown", true).apply()
                        }
                    }
                }
            }
            val collection = remember(context) {
                val preferences = context.getSharedPreferences("analytics_collection", MODE_PRIVATE)
                val seed = preferences.getString("sampling_seed", null) ?: java.util.UUID.randomUUID().toString().also {
                    preferences.edit().putString("sampling_seed", it).apply()
                }
                AnalyticsCollectionController(
                    readCachedPolicy = { preferences.getString("policy", null) },
                    writeCachedPolicy = { preferences.edit().putString("policy", it).apply() },
                    samplingSeed = seed,
                )
            }
            val analytics = remember(context, analyticsEnvironment, collection) {
                val logger = FirebaseAnalyticsEventLogger(FirebaseAnalytics.getInstance(context))
                AnalyticsTracker(
                    environment = analyticsEnvironment,
                    errorSink = PostHogAnalyticsEventLogger::captureUserError,
                    collection = collection,
                    sink = AnalyticsEventDispatcher(logger::log, PostHogAnalyticsEventLogger::log)::log,
                )
            }
            val authPlatform = remember(context, scope, tokenStore) {
                AuthPlatformCallbacks(
                    requestGoogleIdToken = { onResult ->
                        scope.launch {
                            runCatching { requestGoogleIdToken(context) }
                                .onSuccess { onResult(it, null) }
                                 .onFailure { error ->
                                    if (error is CancellationException) throw error
                                    onResult(null, if (error is GetCredentialCancellationException) null else "Google 로그인을 완료하지 못했어요.")
                                }
                        }
                    },
                    readRefreshToken = tokenStore::read,
                    writeRefreshToken = tokenStore::write,
                    clearRefreshToken = tokenStore::clear,
                    readGuest = tokenStore::readGuest,
                    writeGuest = tokenStore::writeGuest,
                    clearGuest = tokenStore::clearGuest,
                    readAnonymousReviews = {
                        val preferences = context.getSharedPreferences("review_preferences", MODE_PRIVATE)
                        if (preferences.contains("anonymous")) preferences.getBoolean("anonymous", true) else null
                    },
                    writeAnonymousReviews = { value ->
                        context.getSharedPreferences("review_preferences", MODE_PRIVATE).edit().putBoolean("anonymous", value).apply()
                    },
                )
            }
            App(
                authPlatform = authPlatform,
                analytics = analytics,
                refreshAnalyticsConfiguration = { PostHogAnalyticsEventLogger.refreshCollectionPolicy(collection) },
                homeHeaderAction = { HomeHeaderAction() },
                onExitRequested = { finish() },
                recentSearchStorage = RecentSearchStorage(
                    read = recentSearchPreferences::read,
                    write = recentSearchPreferences::write,
                ),
                onboardingStorage = OnboardingStorage(
                    hasBeenShown = { onboardingPreferences.getBoolean("first_library_shown", false) },
                    markShown = { onboardingPreferences.edit().putBoolean("first_library_shown", true).apply() },
                ),
            )
        }
    }
}
