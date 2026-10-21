package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRemoteRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveRefreshTest {
    @Test
    fun refreshWaitsForDeletionWithoutCancellingItAndDeduplicatesPulls() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val deletion = CompletableDeferred<Unit>()
        val requests = mutableListOf<HttpMethod>()
        val client = client { method ->
            requests += method
            if (method == HttpMethod.Post) deletion.await()
            BOOKS
        }
        try {
            val viewModel = ArchiveViewModel(LibraryRemoteRepository(client))
            viewModel.authenticate("test-token")
            advanceUntilIdle()
            viewModel.remove(setOf("isbn-1"))
            runCurrent()
            viewModel.refresh()
            viewModel.refresh()
            runCurrent()
            assertEquals(listOf(HttpMethod.Get, HttpMethod.Post), requests)
            assertTrue(viewModel.refreshState.value.isRefreshing)
            deletion.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Get), requests)
            assertEquals(false, viewModel.refreshState.value.isRefreshing)
            assertEquals(1, viewModel.uiState.value.items.size)
        } finally { client.close(); Dispatchers.resetMain() }
    }

    @Test
    fun failureKeepsBooksAndGuestRefreshMakesNoRequests() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var requests = 0
        var fail = false
        val client = client {
            requests++
            if (fail) error("offline")
            BOOKS
        }
        try {
            val viewModel = ArchiveViewModel(LibraryRemoteRepository(client))
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(0, requests)
            viewModel.authenticate("test-token")
            advanceUntilIdle()
            val books = viewModel.uiState.value.items
            fail = true
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(books, viewModel.uiState.value.items)
            assertEquals(true, viewModel.errors.notice.value != null)
            viewModel.authenticate(null)
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(2, requests)
            assertEquals(false, viewModel.refreshState.value.enabled)
        } finally { client.close(); Dispatchers.resetMain() }
    }

    @Test
    fun logoutStillCancelsAnOldMemberWrite() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val pending = CompletableDeferred<Unit>()
        var cancelled = false
        val client = client { method ->
            if (method == HttpMethod.Post) {
                try { pending.await() } finally { cancelled = true }
            }
            BOOKS
        }
        try {
            val viewModel = ArchiveViewModel(LibraryRemoteRepository(client))
            viewModel.authenticate("test-token")
            advanceUntilIdle()
            viewModel.remove(setOf("isbn-1"))
            runCurrent()
            viewModel.authenticate(null)
            advanceUntilIdle()
            assertTrue(cancelled)
            assertTrue(viewModel.uiState.value.items.isEmpty())
            assertEquals(false, viewModel.refreshState.value.enabled)
        } finally { client.close(); Dispatchers.resetMain() }
    }

    private fun client(response: suspend (HttpMethod) -> String) = HttpClient(MockEngine) {
        engine {
            dispatcher = Dispatchers.Main
            addHandler { request ->
                respond(response(request.method), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        install(ContentNegotiation) { json() }
    }

    private companion object {
        const val BOOKS = """{"nextPage":null,"items":[{"bookId":1,"isbn13":"isbn-1","title":"책","coverImageUrl":"","authors":[],"publisher":"출판사","category":"소설","publishedDate":"2026-01-01","totalPages":100,"status":"READING","currentPage":10,"readingUpdatedAt":"2026-01-01T00:00:00Z"}]}"""
    }
}
