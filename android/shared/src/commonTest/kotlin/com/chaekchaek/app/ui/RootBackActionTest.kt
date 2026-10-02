package com.chaekchaek.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class RootBackActionTest {
    @Test
    fun backFromNonHomeTabReturnsToHome() {
        assertEquals(
            RootBackAction.ReturnToHome,
            rootBackAction(
                selectedTab = RootTab.Feed,
                exitConfirmationDeadlineMillis = null,
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun firstBackOnHomeRequestsExitConfirmationForTwoSeconds() {
        assertEquals(
            RootBackAction.ConfirmExit(3_000L),
            rootBackAction(
                selectedTab = RootTab.Home,
                exitConfirmationDeadlineMillis = null,
                nowMillis = 1_000L,
            ),
        )
    }

    @Test
    fun secondBackBeforeConfirmationExpiresExits() {
        assertEquals(
            RootBackAction.Exit,
            rootBackAction(
                selectedTab = RootTab.Home,
                exitConfirmationDeadlineMillis = 3_000L,
                nowMillis = 3_000L,
            ),
        )
    }

    @Test
    fun expiredExitConfirmationRequestsConfirmationAgain() {
        assertEquals(
            RootBackAction.ConfirmExit(5_001L),
            rootBackAction(
                selectedTab = RootTab.Home,
                exitConfirmationDeadlineMillis = 3_000L,
                nowMillis = 3_001L,
            ),
        )
    }
}
