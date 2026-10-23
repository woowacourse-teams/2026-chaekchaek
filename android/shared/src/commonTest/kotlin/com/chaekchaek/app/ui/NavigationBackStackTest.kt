package com.chaekchaek.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class NavigationBackStackTest {
    @Test
    fun sameBookOpenedFromAnotherShelfHasIndependentSerializableEntry() {
        val book = BookDetailArgs(id = "9780000000001", title = "같은 책")
        val first = BookDetailKey(book)
        val second = BookDetailKey(book)
        assertNotEquals(first, second)
        assertNotEquals(first.toString(), second.toString())
        assertEquals(first, Json.decodeFromString<BookDetailKey>(Json.encodeToString(first)))
        val stack = mutableListOf<Any>("root", first, "other-shelf")
        assertTrue(stack.navigateFromCurrent("other-shelf", second))
        assertFalse(stack.popDestinationIfCurrent(first))
        assertTrue(stack.popDestinationIfCurrent(second))
        assertEquals(listOf("root", first, "other-shelf"), stack)
    }

    @Test
    fun reopeningOwnerPopsToTheExistingEntryWithoutReplacingItsState() {
        data class Screen(val id: String, val savedFilter: String = "전체")
        val root = Screen("root")
        val owner = Screen("member-42", "READING")
        val detail = Screen("book-7")
        val stack = mutableListOf(root, owner, Screen("shelf-9"), detail)
        assertTrue(stack.navigateToExistingFromCurrent(detail, Screen("member-42")) { it.id == "member-42" })
        assertEquals(listOf(root, owner), stack)
        assertTrue(stack.last() === owner)
    }
    @Test
    fun staleAndRepeatedPublicProfileCallbacksCannotChangeTheStack() {
        val stack = mutableListOf("root", "book", "member-42")
        assertFalse(stack.navigateToExistingFromCurrent("book", "member-99"))
        assertFalse(stack.navigateToExistingFromCurrent("member-42", "member-42"))
        assertEquals(listOf("root", "book", "member-42"), stack)
    }
    @Test
    fun anotherOwnerIsAddedWithoutDiscardingPreviousOwners() {
        val stack = mutableListOf("root", "member-42", "book")
        assertTrue(stack.navigateToExistingFromCurrent("book", "member-99"))
        assertEquals(listOf("root", "member-42", "book", "member-99"), stack)
    }

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
