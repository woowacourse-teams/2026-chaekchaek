package com.chaekchaek.app.ui.search

import com.chaekchaek.app.domain.book.BookSearchRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun searchSortsByNewestYearAndExposesEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val books = listOf(book("오래된 책", "2021"), book("연도 없음", ""), book("새 책", "2026"))
            val viewModel = SearchViewModel(
                BookSearchRepository { query -> if (query == "없음") emptyList() else books },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.search("없음")
            advanceUntilIdle()
            assertEquals(SearchUiState.Empty, viewModel.uiState.value)

            viewModel.search("책")
            advanceUntilIdle()
            assertEquals(
                listOf("새 책", "오래된 책", "연도 없음"),
                (viewModel.uiState.value as SearchUiState.Success).results.map { it.title },
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun signedOutRegistrationResumesOnlyOnceAfterLogin() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var signedIn = false
            val registered = mutableListOf<BookSearchResult>()
            val viewModel = SearchViewModel(
                BookSearchRepository { emptyList() },
                registerBook = registered::add,
                isSignedIn = { signedIn },
            )
            val result = book("보류할 책", "2026")

            viewModel.register(result)
            assertEquals(result, viewModel.pendingRegistration.value)

            signedIn = true
            viewModel.resumeRegistration()
            viewModel.resumeRegistration()
            advanceUntilIdle()

            assertEquals(null, viewModel.pendingRegistration.value)
            assertEquals(listOf(result), registered)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun book(title: String, year: String) = BookSearchResult(
        title = title,
        creator = "저자",
        publisher = "출판사",
        year = year,
        coverUrl = "",
    )
}
