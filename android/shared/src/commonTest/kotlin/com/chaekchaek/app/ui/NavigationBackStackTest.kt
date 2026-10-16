package com.chaekchaek.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationBackStackTest {
    @Test
    fun rootDestinationCannotBePopped() {
        val backStack = mutableListOf("root")

        assertFalse(backStack.popTopDestination())
        assertEquals(listOf("root"), backStack)
    }

    @Test
    fun repeatedBackFromClosedDestinationDoesNotPopAnotherDestination() {
        val backStack = mutableListOf("root", "detail")

        assertTrue(backStack.popDestinationIfCurrent("detail"))
        assertFalse(backStack.popDestinationIfCurrent("detail"))
        assertEquals(listOf("root"), backStack)
    }

    @Test
    fun callbackFromPreviousDestinationDoesNotPopCurrentDestination() {
        val backStack = mutableListOf("root", "profile")

        assertFalse(backStack.popDestinationIfCurrent("detail"))
        assertEquals(listOf("root", "profile"), backStack)
    }

    @Test
    fun repeatedNavigationFromInactiveDestinationIsIgnored() {
        val backStack = mutableListOf("root")

        assertTrue(backStack.navigateFromCurrent("root", "detail"))
        assertFalse(backStack.navigateFromCurrent("root", "detail"))
        assertEquals(listOf("root", "detail"), backStack)
    }

    @Test
    fun rapidNavigationToDifferentBooksKeepsTheFirstDetail() {
        val root = TestDestination.Root
        val firstDetail = TestDestination.BookDetail("book-a")
        val secondDetail = TestDestination.BookDetail("book-b")
        val backStack = mutableListOf<TestDestination>(root)

        assertTrue(backStack.navigateFromCurrent(root, firstDetail))
        assertFalse(backStack.navigateFromCurrent(root, secondDetail))
        assertEquals(listOf(root, firstDetail), backStack)
    }

    @Test
    fun rapidNavigationAcrossDestinationKindsKeepsTheFirstDestination() {
        val root = TestDestination.Root
        val detail = TestDestination.BookDetail("book-a")
        val detailFirstBackStack = mutableListOf<TestDestination>(root)
        val profileFirstBackStack = mutableListOf<TestDestination>(root)

        assertTrue(detailFirstBackStack.navigateFromCurrent(root, detail))
        assertFalse(detailFirstBackStack.navigateFromCurrent(root, TestDestination.MyPage))
        assertEquals(listOf(root, detail), detailFirstBackStack)

        assertTrue(profileFirstBackStack.navigateFromCurrent(root, TestDestination.MyPage))
        assertFalse(profileFirstBackStack.navigateFromCurrent(root, detail))
        assertEquals(listOf(root, TestDestination.MyPage), profileFirstBackStack)
    }

    @Test
    fun rootBackHandlerIsEnabledOnlyWhileRootIsCurrent() {
        val backStack = mutableListOf("root")

        assertTrue(backStack.isCurrentDestination("root"))

        backStack.add("detail")

        assertFalse(backStack.isCurrentDestination("root"))
    }

    @Test
    fun staleRootExitRequestPopsCurrentDestinationInsteadOfExiting() {
        val backStack = mutableListOf("root", "detail")

        assertFalse(backStack.prepareRootExit("root"))
        assertEquals(listOf("root"), backStack)
    }

    @Test
    fun rootExitRequestExitsOnlyWhileRootIsCurrent() {
        val backStack = mutableListOf("root")

        assertTrue(backStack.prepareRootExit("root"))
        assertEquals(listOf("root"), backStack)
    }

    private sealed interface TestDestination {
        data object Root : TestDestination

        data class BookDetail(val id: String) : TestDestination

        data object MyPage : TestDestination
    }
}
