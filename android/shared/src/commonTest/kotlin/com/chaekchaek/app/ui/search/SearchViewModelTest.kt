package com.chaekchaek.app.ui.search

import androidx.lifecycle.SavedStateHandle
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsTracker
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @Test
    fun failedPaginationKeepsDraftResultsAndCursorUntilExplicitRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<Int>()
            var fail = true
            val handle = SavedStateHandle()
            val vm = SearchViewModel(BookSearchRepository { _, _, page ->
                calls += page
                if (page == 2 && fail) throw IllegalStateException("network")
                BookSearchPage(2, if (page == 1) 2 else null,
                    listOf(book("책 $page", "2026").copy(isbn13 = "isbn$page")))
            }, {}, { true }, savedStateHandle = handle)
            vm.search("실행 검색")
            advanceUntilIdle()
            vm.updateQuery("아직 제출하지 않은 입력")
            vm.loadMore()
            advanceUntilIdle()
            val failed = vm.uiState.value as SearchUiState.Success
            assertEquals(1, failed.results.size)
            assertEquals(2, failed.nextPage)
            assertEquals(false, failed.loadingMore)
            assertEquals(1, handle.get<Int>(SearchSavedStateKeys.LoadedPage))
            assertEquals("아직 제출하지 않은 입력", vm.query.value)
            fail = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 2), calls)
            assertEquals(2, (vm.uiState.value as SearchUiState.Success).results.size)
            assertEquals(2, handle.get<Int>(SearchSavedStateKeys.LoadedPage))
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun failedRestorationRetriesThroughTheSavedPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val calls = mutableListOf<Int>()
            var fail = true
            val handle = SavedStateHandle(mapOf(
                SearchSavedStateKeys.SubmittedQuery to "책",
                SearchSavedStateKeys.LoadedPage to 3,
            ))
            val vm = SearchViewModel(BookSearchRepository { _, _, page ->
                calls += page
                if (page == 2 && fail) error("offline")
                BookSearchPage(3, (page + 1).takeIf { page < 3 },
                    listOf(book("책 $page", "2026").copy(isbn13 = "isbn$page")))
            }, {}, { true }, savedStateHandle = handle)
            advanceUntilIdle()
            assertEquals(listOf(1, 2), calls)
            fail = false
            vm.errors.takeRecovery(vm.errors.notice.value!!.id)?.invoke(null)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 1, 2, 3), calls)
            assertEquals(3, handle.get<Int>(SearchSavedStateKeys.LoadedPage))
            assertEquals(3, (vm.uiState.value as SearchUiState.Success).results.size)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun restorationReloadsThroughThePreviouslyLoadedPageAndDeduplicatesResults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<Int>()
            val savedState = SavedStateHandle(mapOf(
                SearchSavedStateKeys.SubmittedQuery to "책",
                SearchSavedStateKeys.LoadedPage to 3,
            ))
            val viewModel = SearchViewModel(
                bookSearchRepository = BookSearchRepository { _, _, page ->
                    requests += page
                    BookSearchPage(3, (page + 1).takeIf { page < 3 },
                        listOf(book("책 1", "2026").copy(isbn13 = "isbn1"),
                            book("책 $page", "2026").copy(isbn13 = "isbn$page")))
                },
                registerBook = {},
                isSignedIn = { false },
                savedStateHandle = savedState,
            )
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 3), requests)
            assertEquals(3, (viewModel.uiState.value as SearchUiState.Success).results.size)
            viewModel.search("새 검색")
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 3, 1), requests)
            assertEquals(1, savedState.get<Int>(SearchSavedStateKeys.LoadedPage))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoredInputAndSubmittedQueryStaySeparateWithoutRecordingAnotherSubmission() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requestedQueries = mutableListOf<Pair<String, BookSearchSort>>()
            val recentSearchWrites = mutableListOf<List<String>>()
            val analyticsEvents = mutableListOf<AnalyticsEvent>()
            val savedStateHandle = SavedStateHandle(
                mapOf(
                    SearchSavedStateKeys.InputQuery to "아직 검색하지 않은 입력",
                    SearchSavedStateKeys.SubmittedQuery to "마지막 검색",
                    SearchSavedStateKeys.Sort to BookSearchSort.COMMENT.name,
                ),
            )

            val viewModel = SearchViewModel(
                bookSearchRepository = BookSearchRepository { query, sort, _ ->
                    requestedQueries += query to sort
                    BookSearchPage(1, null, listOf(book("복원 결과", "2026")))
                },
                registerBook = {},
                isSignedIn = { true },
                recentSearchStorage = RecentSearchStorage(write = recentSearchWrites::add),
                analytics = AnalyticsTracker("test", analyticsEvents::add),
                savedStateHandle = savedStateHandle,
            )
            advanceUntilIdle()

            assertEquals("아직 검색하지 않은 입력", viewModel.query.value)
            assertEquals(BookSearchSort.COMMENT, viewModel.sort.value)
            assertEquals(listOf("마지막 검색" to BookSearchSort.COMMENT), requestedQueries)
            assertEquals("복원 결과", (viewModel.uiState.value as SearchUiState.Success).results.single().title)
            assertTrue(recentSearchWrites.isEmpty())
            assertTrue(analyticsEvents.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun clearRemovesSubmittedQueryFromSavedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val savedStateHandle = SavedStateHandle()
            var requestCount = 0
            val repository = BookSearchRepository { _, _, _ ->
                requestCount += 1
                BookSearchPage(0, null, emptyList())
            }
            val viewModel = SearchViewModel(repository, {}, { true }, savedStateHandle = savedStateHandle)
            viewModel.search("지울 검색")
            advanceUntilIdle()

            viewModel.clear()
            SearchViewModel(repository, {}, { true }, savedStateHandle = savedStateHandle)
            advanceUntilIdle()

            assertEquals(1, requestCount)
            assertEquals("", savedStateHandle[SearchSavedStateKeys.InputQuery])
            assertEquals("", savedStateHandle[SearchSavedStateKeys.SubmittedQuery])
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun queryAndResultsClearTogether() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = SearchViewModel(
                BookSearchRepository { _, _, _ ->
                    BookSearchPage(1, null, listOf(book("검색 결과", "2026")))
                },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.updateQuery("책")
            viewModel.search(viewModel.query.value)
            advanceUntilIdle()
            assertEquals("책", viewModel.query.value)
            assertEquals("검색 결과", (viewModel.uiState.value as SearchUiState.Success).results.single().title)

            viewModel.clear()

            assertEquals("", viewModel.query.value)
            assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun queryInputAndDirectSearchAreLimitedToOneHundredCharacters() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requestedQueries = mutableListOf<String>()
            val viewModel = SearchViewModel(
                BookSearchRepository { query, _, _ ->
                    requestedQueries += query
                    BookSearchPage(0, null, emptyList())
                },
                registerBook = {},
                isSignedIn = { true },
            )
            val pastedQuery = "책".repeat(SearchInputRules.MAX_QUERY_LENGTH + 50)

            viewModel.updateQuery(pastedQuery)
            viewModel.search(pastedQuery)
            advanceUntilIdle()

            assertEquals(SearchInputRules.MAX_QUERY_LENGTH, viewModel.query.value.length)
            assertEquals(listOf("책".repeat(SearchInputRules.MAX_QUERY_LENGTH)), requestedQueries)
            assertEquals(listOf("책".repeat(SearchInputRules.MAX_QUERY_LENGTH)), viewModel.recentSearches.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun restoredRecentSearchesAreLimitedBeforeDisplay() {
        val pastedQuery = "📚".repeat(SearchInputRules.MAX_QUERY_LENGTH)

        val viewModel = SearchViewModel(
            BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            registerBook = {},
            isSignedIn = { true },
            recentSearchStorage = RecentSearchStorage(read = { listOf(pastedQuery) }),
        )

        assertEquals(
            "📚".repeat(SearchInputRules.MAX_QUERY_LENGTH),
            viewModel.recentSearches.value.single(),
        )
    }

    @Test
    fun queryLimitDoesNotSplitEmojiAfterBmpCharacters() {
        val viewModel = SearchViewModel(
            BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            registerBook = {},
            isSignedIn = { true },
        )

        viewModel.updateQuery("책".repeat(SearchInputRules.MAX_QUERY_LENGTH - 1) + "📚" + "잘림")

        assertEquals(
            "책".repeat(SearchInputRules.MAX_QUERY_LENGTH - 1) + "📚",
            viewModel.query.value,
        )
    }

    @Test
    fun middlePasteAcceptsOnlyAvailableCharactersAndPreservesExistingSuffix() {
        val viewModel = SearchViewModel(
            BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            registerBook = {},
            isSignedIn = { true },
        )
        val current = "책".repeat(SearchInputRules.MAX_QUERY_LENGTH - 5)
        val insertionIndex = 40
        viewModel.updateQuery(current)

        viewModel.updateQuery(
            current.substring(0, insertionIndex) + "📚".repeat(10) + current.substring(insertionIndex),
        )

        assertEquals(
            current.substring(0, insertionIndex) + "📚".repeat(5) + current.substring(insertionIndex),
            viewModel.query.value,
        )
    }

    @Test
    fun directSearchTrimsWhitespaceBeforeApplyingLimit() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requestedQueries = mutableListOf<String>()
            val viewModel = SearchViewModel(
                BookSearchRepository { query, _, _ ->
                    requestedQueries += query
                    BookSearchPage(0, null, emptyList())
                },
                registerBook = {},
                isSignedIn = { true },
            )

            viewModel.search("   " + "책".repeat(SearchInputRules.MAX_QUERY_LENGTH))
            advanceUntilIdle()

            assertEquals(listOf("책".repeat(SearchInputRules.MAX_QUERY_LENGTH)), requestedQueries)
            assertEquals("책".repeat(SearchInputRules.MAX_QUERY_LENGTH), viewModel.query.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

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

    @Test
    fun recentSearchesPersistNewestTenAndMoveDuplicatesToFront() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var persisted = listOf("기존 검색")
            val viewModel = SearchViewModel(
                BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
                registerBook = {},
                isSignedIn = { true },
                recentSearchStorage = RecentSearchStorage(
                    read = { persisted },
                    write = { persisted = it },
                ),
            )

            (1..11).forEach { viewModel.search("검색 $it") }
            viewModel.search("검색 5")
            advanceUntilIdle()

            assertEquals(
                listOf("검색 5", "검색 11", "검색 10", "검색 9", "검색 8", "검색 7", "검색 6", "검색 4", "검색 3", "검색 2"),
                viewModel.recentSearches.value,
            )
            assertEquals(viewModel.recentSearches.value, persisted)
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
