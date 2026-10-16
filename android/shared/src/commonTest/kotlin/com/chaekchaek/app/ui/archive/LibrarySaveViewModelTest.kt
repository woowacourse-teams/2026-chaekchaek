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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
