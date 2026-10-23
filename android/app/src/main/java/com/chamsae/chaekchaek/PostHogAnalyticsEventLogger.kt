package com.chamsae.chaekchaek

import android.content.Context
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsCollectionController
import com.posthog.PersonProfiles
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

internal object PostHogAnalyticsEventLogger {
    private var configured = false

    @Synchronized
    fun configure(context: Context) {
        if (configured || (BuildConfig.DEBUG && !BuildConfig.POSTHOG_CAPTURE_DEBUG)) return
        val token = BuildConfig.POSTHOG_PROJECT_TOKEN.trim()
        if (token.isEmpty()) return
        val host = android.net.Uri.parse(BuildConfig.POSTHOG_HOST)
        if (host.scheme != "https" || host.host.isNullOrBlank()) return
        val config = PostHogAndroidConfig(projectToken = token, host = host.toString()).apply {
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
            captureDeepLinks = false
            captureElementInteractions = false
            captureRageClicks = false
            captureDeadClicks = false
            capturePushNotificationSubscriptions = false
            capturePushNotificationOpened = false
            preloadFeatureFlags = false
            sendFeatureFlagEvent = false
            personProfiles = PersonProfiles.NEVER
            sessionReplay = false
            surveys = false
        }
        runCatching { PostHogAndroid.setup(context.applicationContext, config) }
            .onSuccess { configured = true }
    }

    fun refreshCollectionPolicy(controller: AnalyticsCollectionController) {
        if (!configured) return
        PostHog.reloadFeatureFlags {
            val payload = PostHog.getFeatureFlagPayload(AnalyticsCollectionController.FlagKey)
            if (payload is Map<*, *>) {
                val json = org.json.JSONObject(payload).toString()
                android.os.Handler(android.os.Looper.getMainLooper()).post { controller.receive(json) }
            }
        }
    }

    fun log(event: AnalyticsEvent) {
        if (!configured) return
        val properties = buildMap<String, Any> {
            putAll(event.stringParameters)
            putAll(event.longParameters)
            putAll(event.doubleParameters)
            // PostHog에서 IP 기반 위치 정보 추론을 끈다.
            put("\$geoip_disable", true)
        }
        PostHog.capture(event = event.name, properties = properties)
    }
}
