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
            errorTrackingConfig.autoCapture = true
            errorTrackingConfig.exceptionSteps.enabled = false
            errorTrackingConfig.inAppIncludes.add("com.chaekchaek")
            errorTrackingConfig.inAppIncludes.add("com.chamsae")
            addBeforeSend { event ->
                if (event.event != "\$exception") event else {
                    val clean = com.chaekchaek.app.analytics.sanitizeExceptionProperties(
                        org.json.JSONObject(event.properties.orEmpty()).toString(),
                    )
                    clean?.let { event.copy(properties = jsonProperties(it).toMutableMap()) }
                }
            }
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

    fun captureUserError(propertiesJson: String) {
        if (!configured) return
        PostHog.capture(event = "\$exception", properties = jsonProperties(propertiesJson))
    }

    private fun jsonProperties(json: String): Map<String, Any> {
        fun value(item: Any?): Any = when (item) {
            is org.json.JSONObject -> item.keys().asSequence().associateWith { value(item.get(it)) }
            is org.json.JSONArray -> (0 until item.length()).map { value(item.get(it)) }
            null, org.json.JSONObject.NULL -> ""
            else -> item
        }
        val source = org.json.JSONObject(json)
        return source.keys().asSequence().associateWith { value(source.get(it)) }
    }

    fun log(event: AnalyticsEvent) {
        if (!configured) return
        // 크래시 시점에도 마지막으로 알려진 논리 화면과 행동을 연결한다.
        event.stringParameters["run_id"]?.let { PostHog.register("cc_run_id", it) }
        event.stringParameters["screen_name"]?.let { PostHog.register("cc_screen_name", it) }
        if (event.name !in setOf("cc_app_lifecycle", "cc_view_engagement", "cc_content_impression")) {
            PostHog.register("cc_last_event", event.name)
            event.stringParameters["action"]?.let { PostHog.register("cc_last_action", it) }
        }
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
