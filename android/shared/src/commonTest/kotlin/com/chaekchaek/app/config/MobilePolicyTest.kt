package com.chaekchaek.app.config

import kotlin.test.*

class MobilePolicyTest {
    private val policy = MobilePolicy(androidMinimumVersionCode = 9, androidLatestVersionCode = 11,
        iosMinimumVersion = "1.2.0", iosLatestVersion = "1.10.0", verificationMessage = "원격 확인")

    @Test fun numericVersionsAndBoundariesMatchOnBothPlatforms() {
        assertEquals(UpdateRequirement.Required, InstalledApp(8).requirement(policy))
        assertEquals(UpdateRequirement.Recommended, InstalledApp(9).requirement(policy))
        assertEquals(UpdateRequirement.None, InstalledApp(11).requirement(policy))
        assertEquals(UpdateRequirement.None, InstalledApp(12).requirement(policy))
        assertEquals(UpdateRequirement.Required, InstalledApp(iosVersion = "1.1.9").requirement(policy))
        assertEquals(UpdateRequirement.Recommended, InstalledApp(iosVersion = "1.2").requirement(policy))
        assertEquals(UpdateRequirement.Recommended, InstalledApp(iosVersion = "1.9.0").requirement(policy))
        assertEquals(UpdateRequirement.None, InstalledApp(iosVersion = "1.10.0").requirement(policy))
    }

    @Test fun firstLaunchOfflineUsesSafeDefaults() {
        val controller = MobilePolicyController(InstalledApp(10))
        assertTrue(controller.beginFetch())
        controller.failed("오프라인")
        assertEquals("기본값", controller.state.value.source)
        assertNull(controller.state.value.lastReceivedAt)
        assertEquals(UpdateRequirement.None, controller.state.value.requirement)
        assertFalse(controller.state.value.fetching)
    }

    @Test fun validatedCacheSurvivesProcessRestartAndServerFailure() {
        var saved: String? = null
        val storage = PolicyStorage({ saved }, { saved = it })
        MobilePolicyController(InstalledApp(8), storage).received(MobilePolicy.encode(policy), 123)
        val restarted = MobilePolicyController(InstalledApp(8), storage)
        restarted.failed("서버 장애")
        assertEquals("캐시", restarted.state.value.source)
        assertEquals(123, restarted.state.value.lastReceivedAt)
        assertEquals(policy, restarted.state.value.policy)
        assertEquals(UpdateRequirement.Required, restarted.state.value.requirement)
    }

    @Test fun malformedAndInvertedPoliciesKeepLastAcceptedPolicyAndTimestamp() {
        var saved: String? = null
        val controller = MobilePolicyController(InstalledApp(8), PolicyStorage({ saved }, { saved = it }))
        controller.received(MobilePolicy.encode(policy), 123)
        val previous = saved
        listOf("broken", "[]", "{\"schemaVersion\":2}",
            "{\"androidMinimumVersionCode\":12,\"androidLatestVersionCode\":11}",
            "{\"iosMinimumVersion\":\"1.10.0\",\"iosLatestVersion\":\"1.9.0\"}",
            "{\"iosLatestVersion\":\"latest\"}", "{\"androidMinimumVersionCode\":-1}").forEach {
            controller.received(it, 456)
            assertEquals(previous, saved)
            assertEquals(policy, controller.state.value.policy)
            assertEquals(123, controller.state.value.lastReceivedAt)
            assertEquals(UpdateRequirement.Required, controller.state.value.requirement)
        }
    }

    @Test fun invalidSavedDataFallsBackAndUnknownFieldsAreCompatible() {
        val controller = MobilePolicyController(InstalledApp(10), PolicyStorage({ "not-a-cache" }))
        assertEquals("기본값", controller.state.value.source)
        assertNotNull(MobilePolicy.parse("{\"future\":true}"))
        assertNull(MobilePolicy.parse("{\"verificationMessage\":\"${"x".repeat(201)}\"}"))
        assertNull(AppVersion.parse("1.-2.0"))
        assertNull(AppVersion.parse("1.2.2147483648"))
    }

    @Test fun requestsCoalesceAndFailureAllowsRetry() {
        val controller = MobilePolicyController(InstalledApp(10))
        assertTrue(controller.beginFetch())
        assertFalse(controller.beginFetch())
        controller.failed("오프라인")
        assertTrue(controller.beginFetch())
    }

    @Test fun temporaryOverrideNeverChangesCacheAndResetUsesNewestRemote() {
        var saved: String? = null
        val controller = MobilePolicyController(InstalledApp(10), PolicyStorage({ saved }, { saved = it }))
        controller.received(MobilePolicy.encode(policy), 123)
        controller.overrideRequirement(UpdateRequirement.Required)
        assertEquals("임시", controller.state.value.source)
        controller.received(MobilePolicy.encode(MobilePolicy()), 456)
        assertEquals(UpdateRequirement.Required, controller.state.value.requirement)
        controller.overrideRequirement(null)
        assertEquals(UpdateRequirement.None, controller.state.value.requirement)
        assertEquals("원격", controller.state.value.source)
        assertEquals(MobilePolicy(), MobilePolicy.parse(requireNotNull(saved).substringAfter('\n')))
    }

    @Test fun dismissAppliesOnlyToRecommendedPolicyForThisSession() {
        val controller = MobilePolicyController(InstalledApp(10))
        controller.received(MobilePolicy.encode(policy), 123)
        controller.dismissRecommended()
        controller.received(MobilePolicy.encode(policy), 124)
        assertEquals(UpdateRequirement.None, controller.state.value.requirement)
        controller.received(MobilePolicy.encode(policy.copy(androidMinimumVersionCode = 11)), 125)
        controller.dismissRecommended()
        assertEquals(UpdateRequirement.Required, controller.state.value.requirement)
    }

    @Test fun lateResponseCannotRollBackNewPolicy() {
        val controller = MobilePolicyController(InstalledApp(10))
        controller.received(MobilePolicy.encode(policy), 200)
        controller.received(MobilePolicy.encode(MobilePolicy()), 100)
        assertEquals(policy, controller.state.value.policy)
        assertEquals(200, controller.state.value.lastReceivedAt)
    }
}
