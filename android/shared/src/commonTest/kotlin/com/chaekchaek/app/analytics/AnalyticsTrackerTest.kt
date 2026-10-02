package com.chaekchaek.app.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AnalyticsTrackerTest {
    @Test
    fun addsStableCorrelationContextWithoutAccountIdentifiers() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)

        tracker.updateAuthentication(signedIn = true)
        val flowId = tracker.startFlow("search")
        tracker.log("cc_search_submit", strings = mapOf("search_id" to "search-1"))
        tracker.log("cc_search_result", strings = mapOf("outcome" to "success"))

        assertEquals(2, events.size)
        assertEquals(flowId, events.first().stringParameters["flow_id"])
        assertEquals("search", events.first().stringParameters["origin"])
        assertEquals("signed_in", events.first().stringParameters["auth_state"])
        assertEquals(1L, events.first().longParameters["schema_version"])
        assertEquals(listOf(1L, 2L), events.map { it.longParameters["event_seq"] })
        assertEquals(1, events.map { it.stringParameters["run_id"] }.distinct().size)
        assertNotEquals(
            events[0].stringParameters["event_id"],
            events[1].stringParameters["event_id"],
        )
        assertFalse(events.any { "user_id" in it.stringParameters || "email" in it.stringParameters })
    }

    @Test
    fun actionResultKeepsActionIdAndRecordsOneAttempt() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)

        val action = tracker.startAction("library_add")
        tracker.finishAction(action, "success", strings = mapOf("effect" to "created"))

        assertEquals(listOf("cc_action_start", "cc_action_result"), events.map(AnalyticsEvent::name))
        assertEquals(
            events[0].stringParameters["action_id"],
            events[1].stringParameters["action_id"],
        )
        assertEquals(action.id, events[0].stringParameters["action_id"])
        assertEquals(1L, events[1].longParameters["attempt_count"])
        assertTrue((events[1].longParameters["duration_ms"] ?: -1L) >= 0L)
    }

    @Test
    fun bookSelectionUsesLastVisibleListContextAndCurrentSearch() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)

        tracker.startFlow("search")
        val searchId = tracker.startSearch()
        tracker.screenView("discover")
        tracker.contentImpression("book", "isbn13:123", "isbn13:123", "search_results", 3)
        tracker.bookSelect("isbn13:123")

        val selection = events.last()
        assertEquals("cc_book_select", selection.name)
        assertEquals(searchId, selection.stringParameters["search_id"])
        assertEquals("search_results", selection.stringParameters["list_id"])
        assertEquals(3L, selection.longParameters["position"])
    }

    @Test
    fun ensureFlowKeepsSearchCorrelationWhenBookDetailOpens() {
        val tracker = AnalyticsTracker("test") {}
        val searchFlow = tracker.startFlow("search")

        val detailFlow = tracker.ensureFlow("book_detail")

        assertEquals(searchFlow, detailFlow)
        assertEquals(searchFlow, tracker.currentFlow())
    }

    @Test
    fun newSearchRecordsTheSameBookImpressionAgain() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)
        tracker.screenView("discover")
        tracker.startSearch()
        tracker.contentImpression("book", "isbn13:123", "isbn13:123", "search_results", 0)

        tracker.startSearch()
        tracker.contentImpression("book", "isbn13:123", "isbn13:123", "search_results", 0)

        assertEquals(2, events.count { it.name == "cc_content_impression" })
    }

    @Test
    fun newScreenDoesNotReusePreviousBookPosition() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)
        tracker.screenView("home")
        tracker.contentImpression("book", "isbn13:123", "isbn13:123", "popular_books", 4)

        tracker.screenView("book_detail")
        tracker.bookSelect("isbn13:123")

        val selection = events.last()
        assertFalse("list_id" in selection.stringParameters)
        assertFalse("position" in selection.longParameters)
    }

    @Test
    fun loginAttemptAndComposerKeepTheirCorrelationIds() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)
        val action = tracker.startAction("review_create")
        val attemptId = tracker.beginAuthAttempt("google", "review_create")
        tracker.endAuthAttempt("success")
        val composerId = tracker.openComposer("review", "create")
        tracker.finishAction(action, "success")

        val attemptEvents = events.filter { it.name.startsWith("cc_auth_") }
        assertEquals(2, attemptEvents.size)
        assertTrue(attemptEvents.all { it.stringParameters["auth_attempt_id"] == attemptId })
        assertTrue(attemptEvents.all { it.stringParameters["action_id"] == action.id })
        assertEquals(composerId, events.last().stringParameters["composer_id"])
    }

    @Test
    fun normalizesBookKeysAndBucketsQueryLength() {
        assertEquals("isbn13:9781234567890", analyticsBookKey("978-1-234-56789-0", null))
        assertEquals("catalog:42", analyticsBookKey(null, "42"))
        assertEquals("1_5", analyticsQueryLengthBucket(5))
        assertEquals("6_15", analyticsQueryLengthBucket(6))
        assertEquals("16_plus", analyticsQueryLengthBucket(16))
    }
}
