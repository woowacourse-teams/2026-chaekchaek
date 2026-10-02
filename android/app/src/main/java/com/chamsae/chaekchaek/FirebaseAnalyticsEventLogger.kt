package com.chamsae.chaekchaek

import android.os.Bundle
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.google.firebase.analytics.FirebaseAnalytics

internal class FirebaseAnalyticsEventLogger(
    private val firebaseAnalytics: FirebaseAnalytics,
) {
    fun log(event: AnalyticsEvent) {
        val parameters = Bundle().apply {
            event.stringParameters.forEach(::putString)
            event.longParameters.forEach(::putLong)
            event.doubleParameters.forEach(::putDouble)
        }
        firebaseAnalytics.logEvent(event.name, parameters)
    }
}
