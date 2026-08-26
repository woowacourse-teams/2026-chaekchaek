package com.chaekchaek.app.ui.search

import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchPage
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun searchUsesServerOrderAndExposesEmpty() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val books = listOf(book("오래된 책", "2021"), book("연도 없음", ""), book("새 책", "2026"))
            val viewModel = SearchViewModel(
                BookSearchRepository { query, _, _ ->
                    val items = if (query == "없음") emptyList() else books
                    BookSearchPage(items.size, null, items)
                },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.search("없음")
            advanceUntilIdle()
            assertEquals(SearchUiState.Empty, viewModel.uiState.value)

            viewModel.search("책")
            advanceUntilIdle()
            assertEquals(
                listOf("오래된 책", "연도 없음", "새 책"),
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
                BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
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

    @Test
    fun searchShowsLoadingOnlyAfter500Millis() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = SearchViewModel(
                BookSearchRepository { _, _, _ ->
                    kotlinx.coroutines.delay(501)
                    BookSearchPage(1, null, listOf(book("검색 결과", "2026")))
                },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.search("책")
            runCurrent()
            advanceTimeBy(499)
            runCurrent()
            assertNotEquals(SearchUiState.Loading, viewModel.uiState.value)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(SearchUiState.Loading, viewModel.uiState.value)
            advanceUntilIdle()
            assertEquals("검색 결과", (viewModel.uiState.value as SearchUiState.Success).results.single().title)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sortAndNextPageUseCurrentRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<Pair<BookSearchSort, Int>>()
            val viewModel = SearchViewModel(
                BookSearchRepository { _, sort, page ->
                    requests += sort to page
                    if (page == 1) {
                        BookSearchPage(2, 2, listOf(book("첫 책", "2025").copy(isbn13 = "1")))
                    } else {
                        BookSearchPage(2, null, listOf(book("두 번째 책", "2026").copy(isbn13 = "2")))
                    }
                },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.search("책")
            advanceUntilIdle()
            viewModel.selectSort(BookSearchSort.COMMENT)
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()

            assertEquals(
                listOf(
                    BookSearchSort.LATEST to 1,
                    BookSearchSort.COMMENT to 1,
                    BookSearchSort.COMMENT to 2,
                ),
                requests,
            )
            assertEquals(
                listOf("첫 책", "두 번째 책"),
                (viewModel.uiState.value as SearchUiState.Success).results.map(BookSearchResult::title),
            )
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
