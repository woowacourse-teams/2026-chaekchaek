package com.chaekchaek.app.ui.search

import androidx.lifecycle.SavedStateHandle
import com.chaekchaek.app.analytics.AnalyticsEvent
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchPage
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
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
    fun refreshKeepsDraftSortPagesAndPreviousResultsOnFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = false
            val requests = mutableListOf<Triple<String, BookSearchSort, Int>>()
            val writes = mutableListOf<List<String>>()
            val saved = SavedStateHandle(mapOf(
                SearchSavedStateKeys.SubmittedQuery to "제출",
                SearchSavedStateKeys.InputQuery to "다른 초안",
                SearchSavedStateKeys.Sort to BookSearchSort.COMMENT.name,
                SearchSavedStateKeys.LoadedPage to 2,
            ))
            val viewModel = SearchViewModel(BookSearchRepository { query, sort, page ->
                requests += Triple(query, sort, page)
                if (fail) error("offline")
                BookSearchPage(2, if (page == 1) 2 else null, listOf(book("책 $page", "2026").copy(isbn13 = "$page")))
            }, {}, { true }, recentSearchStorage = RecentSearchStorage(write = writes::add), savedStateHandle = saved)
            advanceUntilIdle()
            val previous = viewModel.uiState.value
            fail = true
            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(previous, viewModel.uiState.value)
            assertEquals("다른 초안", viewModel.query.value)
            assertEquals(BookSearchSort.COMMENT, viewModel.sort.value)
            assertEquals(2, saved.get<Int>(SearchSavedStateKeys.LoadedPage))
            assertEquals(true, viewModel.errors.notice.value != null)
            fail = false
            viewModel.errors.takeRecovery(viewModel.errors.notice.value!!.id)?.invoke(null)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 1, 1, 2), requests.map { it.third })
            assertTrue(requests.all { it.first == "제출" && it.second == BookSearchSort.COMMENT })
            assertTrue(writes.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun paginationFailureWaitsForExplicitRetryAndKeepsCursor() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var fail = true
            val requests = mutableListOf<Int>()
            val viewModel = SearchViewModel(BookSearchRepository { _, _, page ->
                requests += page
                if (page == 2 && fail) error("offline")
                BookSearchPage(2, if (page == 1) 2 else null, listOf(book("책 $page", "2026").copy(isbn13 = "$page")))
            }, {}, { true })
            viewModel.search("책")
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(listOf(1, 2), requests)
            assertTrue((viewModel.uiState.value as SearchUiState.Success).loadMoreFailed)
            fail = false
            viewModel.retryLoadMore()
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 2), requests)
            assertEquals(2, (viewModel.uiState.value as SearchUiState.Success).results.size)
        } finally { Dispatchers.resetMain() }
    }

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
    fun clearingOrChangingSortDuringRefreshDoesNotRestoreTheOldRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val pending = CompletableDeferred<BookSearchPage>()
            var wait = false
            val viewModel = SearchViewModel(BookSearchRepository { _, sort, _ ->
                if (wait && sort == BookSearchSort.LATEST) pending.await()
                else BookSearchPage(1, null, listOf(book(sort.name, "2026")))
            }, {}, { true })
            viewModel.search("책")
            advanceUntilIdle()
            wait = true
            viewModel.refresh()
            runCurrent()
            viewModel.selectSort(BookSearchSort.COMMENT)
            advanceUntilIdle()
            pending.complete(BookSearchPage(0, null, emptyList()))
            advanceUntilIdle()
            assertEquals(BookSearchSort.COMMENT.name, (viewModel.uiState.value as SearchUiState.Success).results.single().title)
            assertEquals(false, viewModel.refreshState.value.isRefreshing)
            viewModel.clear()
            assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun paginationSpinnerStartsAfter500msAndStopsImmediately() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val pending = CompletableDeferred<BookSearchPage>()
            var requests = 0
            val viewModel = SearchViewModel(BookSearchRepository { _, _, page ->
                requests++
                if (page == 2) pending.await()
                else BookSearchPage(2, 2, listOf(book("첫 책", "2026").copy(isbn13 = "1")))
            }, {}, { true })
            viewModel.search("책")
            advanceUntilIdle()
            viewModel.loadMore()
            viewModel.loadMore()
            runCurrent()
            advanceTimeBy(499)
            runCurrent()
            assertEquals(false, (viewModel.uiState.value as SearchUiState.Success).showLoadingMore)
            advanceTimeBy(1)
            runCurrent()
            assertTrue((viewModel.uiState.value as SearchUiState.Success).showLoadingMore)
            pending.complete(BookSearchPage(2, null, listOf(book("둘째 책", "2026").copy(isbn13 = "2"))))
            runCurrent()
            val content = viewModel.uiState.value as SearchUiState.Success
            assertEquals(false, content.loadingMore)
            assertEquals(false, content.showLoadingMore)
            assertEquals(2, requests)
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

    @Test
    fun filteredPagesKeepValidIsbnDeduplicationAndRestoration() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val requests = mutableListOf<Int>()
            var earlierPageChanged = false
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    val page = request.url.parameters["page"]!!.toInt()
                    requests += page
                    val products = when (page) {
                        1 -> listOf("9788937460449")
                        2 -> if (earlierPageChanged) listOf("9788925568683") else listOf("")
                        else -> listOf("9788937460449", "9788936433598")
                    }.joinToString(",") { isbn ->
                        """{"title":"책 $isbn","coverImageUrl":"","authors":[],"translators":[],"publishedDate":null,"isbn13":"$isbn","category":"소설","publisher":"출판사"}"""
                    }
                    respond(
                        """{"totalCount":4,"nextPage":${when (page) { 1 -> 2; 2 -> 4; else -> null }},"items":[$products]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            })) { install(ContentNegotiation) { json() } }
            val repository = BookSearchRemoteRepository(client)
            val handle = SavedStateHandle()
            val vm = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            vm.search("호밀")
            advanceUntilIdle()
            vm.loadMore()
            advanceUntilIdle()

            val content = vm.uiState.value as SearchUiState.Success
            assertEquals(listOf("9788937460449", "9788936433598"), content.results.map { it.isbn13 })
            assertEquals(null, content.nextPage)
            assertTrue(content.results.all { it.year.isEmpty() })
            assertEquals(listOf(1, 2, 4), requests)

            assertEquals(4, handle.get<Int>(SearchSavedStateKeys.LoadedPage))
            earlierPageChanged = true
            val restored = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            advanceUntilIdle()
            val expected = listOf("9788937460449", "9788925568683", "9788936433598")
            assertEquals(expected, (restored.uiState.value as SearchUiState.Success).results.map { it.isbn13 })
            assertEquals(listOf(1, 2, 4, 1, 2, 4), requests)

            vm.refresh()
            advanceUntilIdle()
            assertEquals(expected, (vm.uiState.value as SearchUiState.Success).results.map { it.isbn13 })
            assertEquals(listOf(1, 2, 4, 1, 2, 4, 1, 2, 4), requests)
        } finally { Dispatchers.resetMain() }
    }


    @Test
    fun firstSearchRemembersTheLastPageAfterSkippingExcludedProducts() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var earlierPageChanged = false
            var lastPageExcluded = false
            val requests = mutableListOf<Int>()
            val client = HttpClient(MockEngine(MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { request ->
                    val page = request.url.parameters["page"]!!.toInt()
                    requests += page
                    check(page <= 3) { "복원 범위 밖 페이지는 실패합니다" }
                    val isbn = if (page == 3) {
                        if (lastPageExcluded) "" else "9788936433598"
                    }
                        else if (earlierPageChanged) "9788937460449" else ""
                    respond(
                        """{"totalCount":2,"nextPage":${if (page == 1) 3 else if (lastPageExcluded) 5 else null},"items":[{"title":"책 $isbn","coverImageUrl":"","authors":[],"translators":[],"publishedDate":null,"isbn13":"$isbn","category":"소설","publisher":"출판사"}]}""",
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            })) { install(ContentNegotiation) { json() } }
            val repository = BookSearchRemoteRepository(client)
            val handle = SavedStateHandle()
            val vm = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            vm.search("호밀")
            advanceUntilIdle()
            assertEquals(3, handle.get<Int>(SearchSavedStateKeys.LoadedPage))
            assertEquals(listOf(1, 3), requests)

            earlierPageChanged = true
            vm.refresh()
            advanceUntilIdle()
            val expected = listOf("9788937460449", "9788936433598")
            assertEquals(expected, (vm.uiState.value as SearchUiState.Success).results.map { it.isbn13 })
            val restored = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            advanceUntilIdle()
            assertEquals(expected, (restored.uiState.value as SearchUiState.Success).results.map { it.isbn13 })
            assertEquals(listOf(1, 3, 1, 3, 1, 3), requests)

            lastPageExcluded = true
            vm.refresh()
            advanceUntilIdle()
            val refreshed = vm.uiState.value as SearchUiState.Success
            assertEquals(listOf("9788937460449"), refreshed.results.map { it.isbn13 })
            assertEquals(5, refreshed.nextPage)
            val restoredWithoutLastPage = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            advanceUntilIdle()
            assertEquals(refreshed.results,
                (restoredWithoutLastPage.uiState.value as SearchUiState.Success).results)
            assertEquals(listOf(1, 3, 1, 3, 1, 3, 1, 3, 1, 3), requests)

            earlierPageChanged = false
            requests.clear()
            vm.refresh()
            advanceUntilIdle()
            val emptyWindow = vm.uiState.value as SearchUiState.Success
            assertTrue(emptyWindow.results.isEmpty())
            assertEquals(5, emptyWindow.nextPage)
            val restoredEmptyWindow = SearchViewModel(repository, {}, { true }, savedStateHandle = handle)
            advanceUntilIdle()
            assertEquals(emptyWindow, restoredEmptyWindow.uiState.value)
            assertEquals(listOf(1, 3, 1, 3), requests)
        } finally { Dispatchers.resetMain() }
    }

    private fun book(title: String, year: String) = BookSearchResult(
        title = title,
        creator = "저자",
        publisher = "출판사",
        year = year,
        coverUrl = "",
    )
}
