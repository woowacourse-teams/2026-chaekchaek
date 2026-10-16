package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LibrarySaveViewModelTest {
    private val book = BookDetailArgs(id = "42", bookId = 42, isbn13 = "9780000000042", title = "책")
    private val record = LibraryRecord("READING", 0, null, 42)

    @Test
    fun duplicateTapsAndSavedTapsDoNotRepeatRequests() = runSaveTest {
        val response = CompletableDeferred<LibraryRecord>()
        var requests = 0
        val saved = mutableSetOf<String>()
        val vm = LibrarySaveViewModel(
            addBook = { _, pages, _ -> assertNull(pages); requests++; response.await() },
            onSaved = { b, _ -> saved += b.isbn13 },
            isSaved = saved::contains,
        )
        vm.authenticate("member")
        vm.save(book)
        vm.save(book)
        runCurrent()
        assertEquals(1, requests)
        assertEquals(book.isbn13, vm.uiState.value.savingIsbn)
        advanceTimeBy(499)
        assertFalse(vm.uiState.value.showLoading)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(vm.uiState.value.showLoading)
        response.complete(record)
        advanceUntilIdle()
        assertTrue(book.isbn13 in saved)
        assertFalse(vm.uiState.value.showLoading)
        assertNull(vm.uiState.value.savingIsbn)
        vm.save(book)
        advanceUntilIdle()
        assertEquals(1, requests)
        assertEquals(1, vm.uiState.value.savedCount)
        assertTrue(vm.consumeSaveNotice())
        assertFalse(vm.consumeSaveNotice())
    }

    @Test
    fun loginResumesOnlyTheSelectedBookAndCancellationDiscardsIt() = runSaveTest {
        val calls = mutableListOf<String>()
        val vm = LibrarySaveViewModel({ isbn, _, token -> calls += "$isbn:$token"; record }, { _, _ -> })
        vm.save(book)
        assertEquals(book, vm.uiState.value.pendingBook)
        vm.cancelPendingSave()
        vm.authenticate("member")
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        vm.authenticate(null)
        vm.save(book)
        vm.authenticate("member")
        advanceUntilIdle()
        assertEquals(listOf("${book.isbn13}:member"), calls)
        assertNull(vm.uiState.value.pendingBook)
    }

    @Test
    fun failureDoesNotPublishSavedStateAndCanBeRetried() = runSaveTest {
        var calls = 0
        var saved = 0
        val vm = LibrarySaveViewModel(
            { _, _, _ -> if (++calls == 1) error("offline") else record },
            { _, _ -> saved++ },
        )
        vm.authenticate("member")
        vm.save(book)
        advanceUntilIdle()
        assertEquals(0, saved)
        assertEquals(book, vm.uiState.value.failedBook)
        assertNull(vm.uiState.value.savingIsbn)
        vm.save(book)
        advanceUntilIdle()
        assertEquals(1, saved)
        assertEquals(2, calls)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun responseFromPreviousSessionCannotChangeNewAccount() = runSaveTest {
        val response = CompletableDeferred<LibraryRecord>()
        var saved = 0
        val vm = LibrarySaveViewModel(
            { _, _, _ -> withContext(NonCancellable) { response.await() } },
            { _, _ -> saved++ },
        )
        vm.authenticate("old-member")
        vm.save(book)
        runCurrent()
        vm.authenticate("new-member")
        response.complete(record)
        advanceUntilIdle()
        assertEquals(0, saved)
        assertEquals(0, vm.uiState.value.savedCount)
        assertNull(vm.uiState.value.errorMessage)
    }

    private fun runSaveTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
}
