package com.chaekchaek.app.ui.register

import com.chaekchaek.app.domain.book.BookSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BookRegistrationViewModelTest {
    @Test
    fun resumesPendingRegistrationAfterAuthentication() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val calls = mutableListOf<Pair<String, String>>()
        val viewModel = BookRegistrationViewModel { isbn13, _, token -> calls += isbn13 to token }
        val book = BookSearchResult(
            title = "마션",
            creator = "앤디 위어",
            publisher = "알에이치코리아",
            year = "2026",
            coverUrl = "",
            isbn13 = "9780000000000",
            totalPages = 308,
        )

        try {
            viewModel.register(book)
            assertTrue(viewModel.uiState.value.loginRequired)

            viewModel.authenticate("access-token")
            advanceUntilIdle()

            assertEquals(listOf(book.isbn13 to "access-token"), calls)
            assertFalse(viewModel.uiState.value.loginRequired)
            assertEquals(1, viewModel.uiState.value.completedRegistrationCount)

            viewModel.register(book)
            advanceUntilIdle()

            assertEquals(2, calls.size)
            assertEquals(2, viewModel.uiState.value.completedRegistrationCount)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
