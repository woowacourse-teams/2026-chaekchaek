package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
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
    fun newDetailObserversReceiveOnlySavesCompletedAfterTheirOwnEntry() = runSaveTest {
        val requestedIsbns = mutableListOf<String>()
        val vm = LibrarySaveViewModel(
            addBook = { isbn, _, _ -> requestedIsbns += isbn; record },
            onSaved = { _, _ -> },
        )
        vm.authenticate("member")
        vm.save(book)
        advanceUntilIdle()
        assertEquals(SavedLibraryBook(book.isbn13, record), vm.uiState.value.lastSavedBook)

        val completionFlow = vm.savesCompletedWhileObserving()
        val firstEntry = mutableListOf<SavedLibraryBook>()
        val firstObserver = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            completionFlow.collect { firstEntry += it }
        }
        runCurrent()
        assertTrue(firstEntry.isEmpty())

        val anotherBook = book.copy(id = "99", isbn13 = "9780000000099")
        vm.save(anotherBook)
        advanceUntilIdle()
        assertEquals(listOf(SavedLibraryBook(anotherBook.isbn13, record)), firstEntry)
        firstObserver.cancelAndJoin()

        val reopenedEntry = mutableListOf<SavedLibraryBook>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            completionFlow.collect { reopenedEntry += it }
        }
        runCurrent()
        assertTrue(reopenedEntry.isEmpty())

        vm.save(book)
        advanceUntilIdle()
        assertEquals(listOf(SavedLibraryBook(book.isbn13, record)), reopenedEntry)
        assertEquals(listOf(book.isbn13, anotherBook.isbn13, book.isbn13), requestedIsbns)
        assertEquals(1, firstEntry.size)
    }

    @Test
    fun observingDetailReceivesEachSaveEvenWhenTheCompletedBookAndRecordAreIdentical() = runSaveTest {
        var requests = 0
        val vm = LibrarySaveViewModel(
            addBook = { _, _, _ -> requests += 1; record },
            onSaved = { _, _ -> },
        )
        vm.authenticate("member")
        val completions = mutableListOf<SavedLibraryBook>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.savesCompletedWhileObserving().collect { completions += it }
        }
        vm.save(book)
        advanceUntilIdle()
        // 상세에서 삭제한 뒤 같은 책을 다시 저장하면 서버가 같은 기록을 반환할 수 있다.
        vm.save(book)
        advanceUntilIdle()

        assertEquals(2, requests)
        assertEquals(2, vm.uiState.value.savedCount)
        assertEquals(List(2) { SavedLibraryBook(book.isbn13, record) }, completions)
    }

    @Test
    fun observingDetailReceivesSamePayloadAfterLogoutAndLoginResetTheSaveCount() = runSaveTest {
        val requestTokens = mutableListOf<String>()
        val vm = LibrarySaveViewModel(
            addBook = { _, _, token -> requestTokens += token; record },
            onSaved = { _, _ -> },
        )
        vm.authenticate("old-member")
        val completions = mutableListOf<SavedLibraryBook>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.savesCompletedWhileObserving().collect { completions += it }
        }
        vm.save(book)
        advanceUntilIdle()
        assertEquals(1, completions.size)
        vm.authenticate("old-member-renewed-token")
        runCurrent()
        assertEquals(1, completions.size)

        vm.authenticate(null)
        runCurrent()
        assertEquals(1, completions.size)
        assertEquals(0, vm.uiState.value.savedCount)
        vm.save(book)
        assertEquals(book, vm.uiState.value.pendingBook)
        vm.authenticate("new-member")
        advanceUntilIdle()

        assertEquals(listOf("old-member", "new-member"), requestTokens)
        assertEquals(1, vm.uiState.value.savedCount)
        assertEquals(List(2) { SavedLibraryBook(book.isbn13, record) }, completions)
    }

    @Test
    fun immediateReloginAndSameBookSaveDoNotLoseCompletionWhenStateEndsWithTheSameValues() = runSaveTest {
        val requestTokens = mutableListOf<String>()
        val vm = LibrarySaveViewModel(
            addBook = { _, _, token -> requestTokens += token; record },
            onSaved = { _, _ -> },
        )
        vm.authenticate("old-member")
        val completions = mutableListOf<SavedLibraryBook>()
        backgroundScope.launch(StandardTestDispatcher(testScheduler)) {
            vm.savesCompletedWhileObserving().collect { completions += it }
        }
        runCurrent()
        vm.save(book)
        runCurrent()
        assertEquals(1, completions.size)
        val firstCompletionState = vm.uiState.value

        // 관찰자가 reset 상태를 처리할 차례를 기다리지 않고 새 계정의 같은 책 저장을 완료한다.
        vm.authenticate(null)
        vm.authenticate("new-member")
        vm.save(book)
        runCurrent()

        assertEquals(firstCompletionState, vm.uiState.value)
        assertEquals(listOf("old-member", "new-member"), requestTokens)
        assertEquals(List(2) { SavedLibraryBook(book.isbn13, record) }, completions)
    }

    @Test
    fun `로그인 후 이미 저장된 책을 담으면 기존 기록으로 저장 상태를 알린다`() = runSaveTest {
        val requests = mutableListOf<HttpMethod>()
        val client = HttpClient(MockEngine { request ->
            requests += request.method
            assertEquals("Bearer member", request.headers[HttpHeaders.Authorization])
            if (request.method == HttpMethod.Post) {
                respond(
                    """{"code":"LIBRARY_ITEM_ALREADY_EXISTS"}""",
                    HttpStatusCode.Conflict,
                    headersOf(HttpHeaders.ContentType, "application/problem+json"),
                )
            } else {
                respond(
                    """{"bookId":42,"isbn13":"9780000000042","title":"책","authors":[],"translators":[],"publisher":"출판사","category":"소설","coverImageUrl":"","myRecord":{"status":"FINISHED","currentPage":308,"myRating":4.5}}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        try {
            val repository = BookDetailRemoteRepository(client)
            val saved = mutableMapOf<String, LibraryRecord>()
            val vm = LibrarySaveViewModel(
                addBook = repository::addToLibrary,
                onSaved = { book, record -> saved[book.isbn13] = record },
                isSaved = saved::containsKey,
            )
            vm.save(book)
            assertEquals(book, vm.uiState.value.pendingBook)

            vm.authenticate("member")
            withContext(Dispatchers.Default) {
                withTimeout(5_000) { vm.uiState.first { it.savedCount == 1 || it.errorMessage != null } }
            }
            advanceUntilIdle()

            assertEquals(LibraryRecord("FINISHED", 308, 4.5, 42), saved[book.isbn13])
            assertEquals(1, vm.uiState.value.savedCount)
            assertNull(vm.uiState.value.pendingBook)
            assertNull(vm.uiState.value.errorMessage)
            assertNull(vm.uiState.value.failedBook)
            assertNull(vm.uiState.value.savingIsbn)
            assertTrue(vm.consumeSaveNotice())
            vm.save(book)
            advanceUntilIdle()
            assertEquals(listOf(HttpMethod.Post, HttpMethod.Get), requests)
        } finally {
            client.close()
        }
    }

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
    fun tokenRenewalKeepsTheInFlightSaveAndUsesTheNewTokenForTheNextSave() = runSaveTest {
        val response = CompletableDeferred<LibraryRecord>()
        val tokens = mutableListOf<String>()
        var saved = 0
        val vm = LibrarySaveViewModel(
            { _, _, token -> tokens += token; response.await() },
            { _, _ -> saved++ },
        )
        vm.authenticate("member-old-token")
        vm.save(book)
        runCurrent()
        vm.authenticate("member-renewed-token")
        assertEquals(book.isbn13, vm.uiState.value.savingIsbn)
        response.complete(record)
        advanceUntilIdle()
        assertEquals(1, saved)
        assertEquals(1, vm.uiState.value.savedCount)
        assertEquals(SavedLibraryBook(book.isbn13, record), vm.uiState.value.lastSavedBook)
        vm.save(book.copy(isbn13 = "9780000000099"))
        advanceUntilIdle()
        assertEquals(listOf("member-old-token", "member-renewed-token"), tokens)
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
        vm.authenticate(null)
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
