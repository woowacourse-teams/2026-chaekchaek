package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
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
class ArchiveMembershipTest {
    @Test
    fun tokenRenewalKeepsMembershipWhenRefreshFailsButNewLoginStartsEmpty() = runTest {
        var failRefresh = false
        val engine = MockEngine {
            if (failRefresh) error("offline")
            respond(libraryJson(listOf(1)), headers = jsonHeaders())
        }
        withArchive(engine) { vm ->
            vm.authenticate("member-old-token")
            awaitReal { vm.uiState.first { it.items.isNotEmpty() } }
            failRefresh = true
            vm.authenticate("member-renewed-token")
            assertEquals(listOf(isbn(1)), vm.uiState.value.items.map { it.id })
            awaitReal { vm.uiState.first { it.errorMessage != null } }
            assertEquals(listOf(isbn(1)), vm.uiState.value.items.map { it.id })

            vm.authenticate(null)
            assertTrue(vm.uiState.value.items.isEmpty())
            assertFalse(vm.uiState.value.showLoading)
            vm.authenticate("another-member")
            awaitReal { vm.uiState.first { it.errorMessage != null } }
            assertTrue(vm.uiState.value.items.isEmpty())
        }
    }

    @Test
    fun membershipRefreshAndTokenRenewalDoNotCancelChunkedWritesOrQueuedMutations() = runTest {
        val secondDeleteStarted = CompletableDeferred<Unit>()
        val finishSecondDelete = CompletableDeferred<Unit>()
        var deleteCalls = 0
        var completedDeletes = 0
        var statusCalls = 0
        var refreshedAfterWrites = false
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("bulk-delete") -> {
                    deleteCalls += 1
                    if (deleteCalls == 2) {
                        secondDeleteStarted.complete(Unit)
                        finishSecondDelete.await()
                    }
                    completedDeletes += 1
                    respond("", HttpStatusCode.NoContent)
                }
                request.url.encodedPath.endsWith("bulk-status") -> {
                    assertEquals(2, completedDeletes)
                    statusCalls += 1
                    respond("", HttpStatusCode.NoContent)
                }
                else -> {
                    val ids = if (statusCalls == 0) (1..13).toList() else listOf(12, 42)
                    if (statusCalls > 0) refreshedAfterWrites = true
                    respond(libraryJson(ids), headers = jsonHeaders())
                }
            }
        }
        withArchive(engine) { vm ->
            vm.authenticate("member-old-token")
            awaitReal { vm.uiState.first { it.items.size == 13 } }
            vm.remove((1..11).mapTo(mutableSetOf(), ::isbn))
            awaitReal { secondDeleteStarted.await() }
            vm.changeStatus(setOf(isbn(12)), ReadingStatus.FINISHED)
            val savedBook = BookDetailArgs("42", isbn13 = isbn(42), title = "저장한 책")
            vm.acceptSavedBook(savedBook, LibraryRecord("READING", 0, null, 42))
            vm.acceptRemovedBook(isbn(13))
            vm.retry()
            vm.authenticate("member-renewed-token")
            runCurrent()
            advanceTimeBy(500)
            runCurrent()
            assertEquals(0, statusCalls)
            assertEquals(1, completedDeletes)
            assertTrue(vm.uiState.value.showLoading)
            assertTrue(vm.uiState.value.items.any { it.id == isbn(42) })
            assertFalse(vm.uiState.value.items.any { it.id == isbn(13) })

            finishSecondDelete.complete(Unit)
            awaitReal { vm.uiState.first { it.items.map { book -> book.id } == listOf(isbn(12), isbn(42)) } }
            assertEquals(2, completedDeletes)
            assertEquals(1, statusCalls)
            assertTrue(refreshedAfterWrites)
            assertFalse(vm.uiState.value.showLoading)
            assertNull(vm.uiState.value.errorMessage)
        }
    }

    @Test
    fun logoutCancelsQueuedWritesAndLateOldSessionResponseCannotChangeNewAccount() = runTest {
        val deleteStarted = CompletableDeferred<Unit>()
        val finishOldDelete = CompletableDeferred<Unit>()
        var statusCalls = 0
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("bulk-delete") -> {
                    deleteStarted.complete(Unit)
                    withContext(NonCancellable) { finishOldDelete.await() }
                    respond("", HttpStatusCode.NoContent)
                }
                request.url.encodedPath.endsWith("bulk-status") -> {
                    statusCalls += 1
                    respond("", HttpStatusCode.NoContent)
                }
                else -> {
                    val ids = if (request.headers[HttpHeaders.Authorization] == "Bearer new-member") listOf(77) else listOf(1, 2)
                    respond(libraryJson(ids), headers = jsonHeaders())
                }
            }
        }
        withArchive(engine) { vm ->
            vm.authenticate("old-member")
            awaitReal { vm.uiState.first { it.items.size == 2 } }
            vm.remove(setOf(isbn(1)))
            awaitReal { deleteStarted.await() }
            vm.changeStatus(setOf(isbn(2)), ReadingStatus.FINISHED)
            advanceTimeBy(500)
            runCurrent()
            assertTrue(vm.uiState.value.showLoading)
            vm.authenticate(null)
            vm.authenticate("new-member")
            assertTrue(vm.uiState.value.items.isEmpty())
            assertFalse(vm.uiState.value.showLoading)

            finishOldDelete.complete(Unit)
            awaitReal { vm.uiState.first { it.items.singleOrNull()?.id == isbn(77) } }
            assertEquals(0, statusCalls)
            assertNull(vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.showLoading)
        }
    }

    @Test
    fun replacingSlowRefreshClearsOnlyTheCancelledRequestsLoading() = runTest {
        val firstRefreshStarted = CompletableDeferred<Unit>()
        val replacementStarted = CompletableDeferred<Unit>()
        val finishReplacement = CompletableDeferred<Unit>()
        var getCalls = 0
        val engine = MockEngine(MockEngineConfig().apply {
            dispatcher = StandardTestDispatcher(testScheduler)
            addHandler {
                when (++getCalls) {
                    2 -> {
                        firstRefreshStarted.complete(Unit)
                        CompletableDeferred<Unit>().await()
                    }
                    3 -> {
                        replacementStarted.complete(Unit)
                        finishReplacement.await()
                    }
                }
                respond(libraryJson(listOf(1)), headers = jsonHeaders())
            }
        })
        withArchive(engine) { vm ->
            vm.authenticate("member")
            runCurrent()
            assertEquals(1, vm.uiState.value.items.size)
            vm.retry()
            runCurrent()
            assertTrue(firstRefreshStarted.isCompleted)
            advanceTimeBy(500)
            runCurrent()
            assertTrue(vm.uiState.value.showLoading)
            vm.retry()
            runCurrent()
            assertTrue(replacementStarted.isCompleted)
            assertFalse(vm.uiState.value.showLoading)
            advanceTimeBy(500)
            runCurrent()
            assertTrue(vm.uiState.value.showLoading)
            finishReplacement.complete(Unit)
            runCurrent()
            assertFalse(vm.uiState.value.showLoading)
            assertEquals(listOf(isbn(1)), vm.uiState.value.items.map { it.id })
            assertNull(vm.uiState.value.errorMessage)
        }
    }

    @Test
    fun lateReadCannotEraseConfirmedMembershipWhenTheReplacementRefreshFails() = runTest {
        val oldReadStarted = CompletableDeferred<Unit>()
        val finishOldRead = CompletableDeferred<Unit>()
        var getCalls = 0
        val engine = MockEngine {
            when (++getCalls) {
                2 -> {
                    oldReadStarted.complete(Unit)
                    withContext(NonCancellable) { finishOldRead.await() }
                }
                3 -> error("offline")
            }
            respond(libraryJson(listOf(1)), headers = jsonHeaders())
        }
        withArchive(engine) { vm ->
            vm.authenticate("member")
            awaitReal { vm.uiState.first { it.items.size == 1 } }
            vm.retry()
            awaitReal { oldReadStarted.await() }
            vm.acceptSavedBook(
                BookDetailArgs("42", isbn13 = isbn(42), title = "저장한 책"),
                LibraryRecord("READING", 0, null, 42),
            )
            finishOldRead.complete(Unit)
            awaitReal { vm.uiState.first { it.errorMessage != null } }
            assertEquals(listOf(isbn(1), isbn(42)), vm.uiState.value.items.map { it.id })
            assertFalse(vm.uiState.value.showLoading)
        }
    }

    @Test
    fun savedAndRemovedMembershipIsImmediateEvenWhenRefreshFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var failRefresh = false
        val client = HttpClient(MockEngine {
            if (failRefresh) error("offline")
            respond("""{"items":[],"nextPage":null}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val vm = ArchiveViewModel(LibraryRemoteRepository(client))
        try {
            vm.authenticate("member")
            advanceUntilIdle()
            failRefresh = true
            val book = BookDetailArgs("42", isbn13 = "9780000000042", title = "책")
            vm.acceptSavedBook(book, LibraryRecord("READING", 0, null, 42))
            assertEquals(listOf(book.isbn13), vm.uiState.value.items.map { it.id })
            withContext(Dispatchers.Default) {
                withTimeout(5_000) { vm.uiState.first { it.errorMessage != null } }
            }
            assertEquals(listOf(book.isbn13), vm.uiState.value.items.map { it.id })
            vm.acceptRemovedBook(book.isbn13)
            assertTrue(vm.uiState.value.items.isEmpty())
            vm.authenticate(null)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.items.isEmpty())
        } finally {
            vm.authenticate(null)
            advanceUntilIdle()
            client.close()
            Dispatchers.resetMain()
        }
    }

    private suspend fun TestScope.withArchive(engine: MockEngine, block: suspend (ArchiveViewModel) -> Unit) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val vm = ArchiveViewModel(LibraryRemoteRepository(client))
        try {
            block(vm)
        } finally {
            vm.authenticate(null)
            advanceUntilIdle()
            client.close()
            Dispatchers.resetMain()
        }
    }

    private suspend fun <T> awaitReal(block: suspend () -> T): T =
        withContext(Dispatchers.Default) { withTimeout(5_000) { block() } }

    private fun isbn(id: Int) = "978" + id.toString().padStart(10, '0')

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private fun libraryJson(ids: List<Int>): String = ids.joinToString(
        prefix = """{"nextPage":null,"items":[""", postfix = "]}",
    ) { id ->
        """{"bookId":$id,"isbn13":"${isbn(id)}","title":"책 $id","coverImageUrl":"cover","authors":[],"publisher":"출판사","category":"소설","publishedDate":"2026-01-01","totalPages":100,"status":"READING","currentPage":0,"readingUpdatedAt":"2026-01-01T00:00:00Z"}"""
    }
}
