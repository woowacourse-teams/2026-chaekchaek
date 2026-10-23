package com.chaekchaek.app.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AnalyticsEventDispatcherTest {
    private val event = AnalyticsEvent(
        "cc_action_result",
        mapOf("event_id" to "event-1", "outcome" to "success"),
        mapOf("duration_ms" to 42L),
        mapOf("rating" to 4.5),
    )

    @Test
    fun sendsTheSameEventOnceToBothProviders() {
        val primaryEvents = mutableListOf<AnalyticsEvent>()
        val secondaryEvents = mutableListOf<AnalyticsEvent>()
        AnalyticsEventDispatcher(primaryEvents::add, secondaryEvents::add).log(event)
        assertEquals(listOf(event), primaryEvents)
        assertEquals(listOf(event), secondaryEvents)
        assertSame(primaryEvents.single(), secondaryEvents.single())
    }

    @Test
    fun primaryFailureDoesNotPreventSecondaryDelivery() {
        val events = mutableListOf<AnalyticsEvent>()
        AnalyticsEventDispatcher({ error("provider unavailable") }, events::add).log(event)
        assertEquals(listOf(event), events)
    }

    @Test
    fun secondaryFailureDoesNotEscapeOrPreventLaterDelivery() {
        val events = mutableListOf<AnalyticsEvent>()
        val dispatcher = AnalyticsEventDispatcher(events::add) { error("provider unavailable") }
        dispatcher.log(event)
        dispatcher.log(event)
        assertEquals(listOf(event, event), events)
    }
}
