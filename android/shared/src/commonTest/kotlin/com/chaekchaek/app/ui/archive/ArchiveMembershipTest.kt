package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveMembershipTest {
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
}
