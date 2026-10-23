package com.chamsae.chaekchaek

import android.os.Bundle
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.firebaseAnalyticsEvent
import com.google.firebase.analytics.FirebaseAnalytics

internal class FirebaseAnalyticsEventLogger(
    private val firebaseAnalytics: FirebaseAnalytics,
) {
    fun log(event: AnalyticsEvent) {
        val compatible = firebaseAnalyticsEvent(event)
        val parameters = Bundle().apply {
            compatible.stringParameters.forEach(::putString)
            compatible.longParameters.forEach(::putLong)
            compatible.doubleParameters.forEach(::putDouble)
        }
        firebaseAnalytics.logEvent(event.name, parameters)
    }
}
