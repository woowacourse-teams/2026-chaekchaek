package com.chaekchaek.app.analytics

import kotlin.test.*
import kotlin.time.TestTimeSource
import kotlin.time.Duration.Companion.seconds

class AnalyticsCollectionControllerTest {
    private val event = AnalyticsEvent("cc_action_start", emptyMap(), emptyMap(), emptyMap())
    private fun policy(enabled: Boolean = true, rate: Double = 1.0, events: String = "{}", revision: String = "v1") =
        """{"schema_version":1,"revision":"$revision","enabled":$enabled,"sample_rate":$rate,"events":$events}"""

    @Test fun disabledPolicySurvivesRestartAndInvalidResponses() {
        var cached: String? = null
        val controller = AnalyticsCollectionController(writeCachedPolicy = { cached = it })
        assertTrue(controller.receive(policy(enabled = false)))
        listOf("", "{}", "null", "broken", policy(rate = -1.0), policy(rate = 1.1), policy(revision = "bad revision"), policy().replace("schema_version\":1", "schema_version\":2")).forEach {
            assertFalse(controller.receive(it), it)
            assertNull(controller.filter(event))
        }
        assertNull(AnalyticsCollectionController(readCachedPolicy = { cached }).filter(event))
        assertTrue(controller.receive(policy()))
        assertEquals("v1", controller.filter(event)?.stringParameters?.get("collection_revision"))
    }

    @Test fun eventRulesOverrideSamplingButCannotOverrideGlobalStop() {
        val controller = AnalyticsCollectionController()
        assertTrue(controller.receive(policy(rate = 0.0, events = """{"cc_action_start":{"sample_rate":1},"cc_action_result":{"enabled":false}}""")))
        assertNotNull(controller.filter(event))
        assertNull(controller.filter(event.copy(name = "cc_action_result")))
        assertNull(controller.filter(event.copy(name = "screen_view")))
        assertTrue(controller.receive(policy(enabled = false, events = """{"cc_action_start":{"enabled":true,"sample_rate":1}}""")))
        assertNull(controller.filter(event))
    }

    @Test fun installationSampleIsStableAcrossEventsAndRestart() {
        var included = 0
        repeat(1_000) { seed ->
            val payload = policy(rate = 0.3)
            val first = AnalyticsCollectionController(readCachedPolicy = { payload }, samplingSeed = "installation-$seed")
            val restarted = AnalyticsCollectionController(readCachedPolicy = { payload }, samplingSeed = "installation-$seed")
            val accepted = first.filter(event) != null
            assertEquals(accepted, first.filter(event.copy(name = "cc_action_result")) != null)
            assertEquals(accepted, restarted.filter(event) != null)
            if (accepted) included++
        }
        assertTrue(included in 150..450, "included=$included")
    }

    @Test fun sharedGateControlsBothProvidersAndCanResumeWithoutRestart() {
        val primary = mutableListOf<AnalyticsEvent>()
        val secondary = mutableListOf<AnalyticsEvent>()
        val controller = AnalyticsCollectionController()
        val tracker = AnalyticsTracker("test", AnalyticsEventDispatcher(primary::add, secondary::add)::log, controller)
        controller.receive(policy(enabled = false))
        tracker.log("cc_action_start")
        assertTrue(primary.isEmpty() && secondary.isEmpty())
        controller.receive(policy())
        tracker.log("cc_action_result")
        assertEquals(primary, secondary)
        assertEquals(1, primary.size)
    }

    @Test fun engagementExcludesBackgroundAndKeepsEachScreenContext() {
        val time = TestTimeSource()
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add, AnalyticsCollectionController(), time)
        tracker.screenView("home")
        tracker.setForeground(true)
        time += 10.seconds
        tracker.screenView("feed")
        time += 5.seconds
        tracker.setForeground(false)
        time += 60.seconds
        tracker.setForeground(true)
        time += 3.seconds
        tracker.recordViewEngagement()
        val engagement = events.filter { it.name == "cc_view_engagement" }
        assertEquals(listOf("home", "feed", "feed"), engagement.map { it.stringParameters["screen_name"] })
        assertEquals(listOf(10_000L, 5_000L, 3_000L), engagement.map { it.longParameters["active_ms"] })
        assertEquals(listOf(1L, 1L, 2L), engagement.map { it.longParameters["segment_index"] })
        assertEquals("home", events.last().stringParameters["previous_screen"])
    }

    @Test fun firebaseProjectionDoesNotDiscardRichPostHogEvent() {
        val events = mutableListOf<AnalyticsEvent>()
        val tracker = AnalyticsTracker("test", events::add)
        tracker.screenView("book_detail")
        tracker.startFlow("search")
        tracker.startSearch()
        val action = tracker.startAction("review_create")
        tracker.openComposer("review", "create")
        tracker.finishAction(action, "success", strings = (1..20).associate { "extra_$it" to "value" })
        val full = events.last()
        val firebase = firebaseAnalyticsEvent(full)
        assertTrue(full.stringParameters.size + full.longParameters.size + full.doubleParameters.size > 25)
        assertEquals(25, firebase.stringParameters.size + firebase.longParameters.size + firebase.doubleParameters.size)
        listOf("event_id", "action_id", "action", "outcome", "collection_revision").forEach {
            assertEquals(full.stringParameters[it], firebase.stringParameters[it])
        }
        assertEquals(1.0, firebase.doubleParameters["sample_rate"])
        assertTrue(full.stringParameters.containsKey("extra_20"))
    }
}
