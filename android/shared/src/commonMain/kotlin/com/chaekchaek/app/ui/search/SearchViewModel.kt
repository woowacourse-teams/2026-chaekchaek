package com.chaekchaek.app.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.analytics.analyticsQueryLengthBucket
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.ui.input.TextInputLimit
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data object Empty : SearchUiState
    data class Success(
        val results: List<BookSearchResult>,
        val totalCount: Int,
        val nextPage: Int?,
        val loadingMore: Boolean = false,
    ) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
    private val bookSearchRepository: BookSearchRepository,
    registerBook: (BookSearchResult) -> Unit,
    isSignedIn: () -> Boolean,
    private val recentSearchStorage: RecentSearchStorage = RecentSearchStorage(),
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val _sort = MutableStateFlow(
        BookSearchSort.entries.firstOrNull { it.name == savedStateHandle[SearchSavedStateKeys.Sort] }
            ?: BookSearchSort.LATEST,
    )
    val sort: StateFlow<BookSearchSort> = _sort.asStateFlow()
    private val _query = MutableStateFlow(
        SearchInputRules.limit(savedStateHandle.get<String>(SearchSavedStateKeys.InputQuery).orEmpty()),
    )
    val query: StateFlow<String> = _query.asStateFlow()
    private val _recentSearches = MutableStateFlow(
        recentSearchStorage.read()
            .map(String::trim)
            .map(SearchInputRules::limit)
            .filter(String::isNotEmpty)
            .distinct()
            .take(RECENT_SEARCH_LIMIT),
    )
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()
    private val _pendingRegistration = MutableStateFlow<BookSearchResult?>(null)
    val pendingRegistration: StateFlow<BookSearchResult?> = _pendingRegistration.asStateFlow()
    private var searchJob: Job? = null
    private var currentQuery = SearchInputRules.limit(
        savedStateHandle.get<String>(SearchSavedStateKeys.SubmittedQuery).orEmpty(),
    )
    private var registerBook = registerBook
    private var isSignedIn = isSignedIn

    init {
        if (currentQuery.isNotEmpty()) search(currentQuery, trigger = "restore", trackAnalytics = false)
    }

    fun attachRegistration(
        registerBook: (BookSearchResult) -> Unit,
        isSignedIn: () -> Boolean,
    ) {
        this.registerBook = registerBook
        this.isSignedIn = isSignedIn
    }

    fun clear() {
        searchJob?.cancel()
        currentQuery = ""
        _query.value = ""
        _uiState.value = SearchUiState.Idle
        savedStateHandle[SearchSavedStateKeys.InputQuery] = ""
        savedStateHandle[SearchSavedStateKeys.SubmittedQuery] = ""
        savedStateHandle[SearchSavedStateKeys.LoadedPage] = FIRST_PAGE
    }

    fun updateQuery(query: String) {
        _query.value = SearchInputRules.applyChange(_query.value, query)
        savedStateHandle[SearchSavedStateKeys.InputQuery] = _query.value
    }

    fun search(query: String) = search(query, "submit")

    private fun search(query: String, trigger: String, trackAnalytics: Boolean = true) {
        val trimmed = SearchInputRules.limit(query.trim())
        if (trimmed.isEmpty()) return
        val restoreThroughPage = if (trigger == "restore") {
            savedStateHandle.get<Int>(SearchSavedStateKeys.LoadedPage) ?: FIRST_PAGE
        } else FIRST_PAGE
        if (trigger != "restore") savedStateHandle[SearchSavedStateKeys.LoadedPage] = FIRST_PAGE
        if (trigger == "submit") {
            _query.value = trimmed
            savedStateHandle[SearchSavedStateKeys.InputQuery] = trimmed
            recordRecentSearch(trimmed)
            analytics.startFlow("search")
        }
        val searchId = if (trackAnalytics) analytics.startSearch() else null
        val searchStarted = if (trackAnalytics) TimeSource.Monotonic.markNow() else null
        if (searchId != null) {
            analytics.log(
                name = "cc_search_submit",
                strings = mapOf(
                    "search_id" to searchId,
                    "query_length_bucket" to analyticsQueryLengthBucket(trimmed.length),
                    "sort" to _sort.value.name.lowercase(),
                    "trigger" to trigger,
                ),
            )
        }
        currentQuery = trimmed
        savedStateHandle[SearchSavedStateKeys.SubmittedQuery] = trimmed
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = try {
                val page = withDelayedApiLoading(
                    onLoadingChanged = { loading ->
                        if (loading) _uiState.value = SearchUiState.Loading
                    },
                ) {
                    var page = bookSearchRepository.search(trimmed, _sort.value, FIRST_PAGE)
                    var loadedPage = FIRST_PAGE
                    while (true) {
                        val nextPage = page.nextPage?.takeIf { it > loadedPage && it <= restoreThroughPage }
                            ?: break
                        val next = bookSearchRepository.search(trimmed, _sort.value, nextPage)
                        page = next.copy(items = (page.items + next.items).distinctBy(BookSearchResult::isbn13))
                        loadedPage = nextPage
                    }
                    savedStateHandle[SearchSavedStateKeys.LoadedPage] = loadedPage
                    page
                }
                if (searchId != null && searchStarted != null) {
                    analytics.log(
                        name = "cc_search_result",
                        strings = mapOf(
                            "search_id" to searchId,
                            "outcome" to if (page.items.isEmpty()) "empty" else "success",
                        ),
                        longs = mapOf(
                            "result_count" to page.totalCount.toLong(),
                            "duration_ms" to searchStarted.elapsedNow().inWholeMilliseconds,
                        ),
                    )
                }
                if (page.items.isEmpty()) SearchUiState.Empty else SearchUiState.Success(
                    page.items,
                    page.totalCount,
                    page.nextPage,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (searchId != null && searchStarted != null) {
                    analytics.log(
                        name = "cc_search_result",
                        strings = mapOf(
                            "search_id" to searchId,
                            "outcome" to "failure",
                            "error_category" to analyticsErrorCategory(error),
                        ),
                        longs = mapOf("duration_ms" to searchStarted.elapsedNow().inWholeMilliseconds),
                    )
                }
                SearchUiState.Error(error.message ?: "검색 중 오류가 발생했습니다")
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value as? SearchUiState.Success ?: return
        val page = current.nextPage ?: return
        if (current.loadingMore) return
        val query = currentQuery
        val sort = _sort.value
        _uiState.value = current.copy(loadingMore = true)
        searchJob = viewModelScope.launch {
            try {
                val next = bookSearchRepository.search(query, sort, page)
                val latest = _uiState.value as? SearchUiState.Success ?: return@launch
                if (currentQuery != query || _sort.value != sort || latest.nextPage != page) return@launch
                savedStateHandle[SearchSavedStateKeys.LoadedPage] = page
                _uiState.value = latest.copy(
                    results = (latest.results + next.items).distinctBy(BookSearchResult::isbn13),
                    totalCount = next.totalCount,
                    nextPage = next.nextPage,
                    loadingMore = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                val latest = _uiState.value as? SearchUiState.Success ?: return@launch
                if (latest.nextPage == page) _uiState.value = latest.copy(loadingMore = false)
            }
        }
    }

    fun selectSort(sort: BookSearchSort) {
        if (_sort.value == sort) return
        _sort.value = sort
        savedStateHandle[SearchSavedStateKeys.Sort] = sort.name
        if (currentQuery.isNotEmpty()) search(currentQuery, "sort_change")
    }

    fun register(book: BookSearchResult) {
        registerBook(book)
        if (!isSignedIn()) {
            _pendingRegistration.value = book
            return
        }
    }

    fun resumeRegistration() {
        if (_pendingRegistration.value == null) return
        _pendingRegistration.value = null
    }

    fun cancelRegistration() {
        _pendingRegistration.value = null
    }

    private fun recordRecentSearch(query: String) {
        val updated = (listOf(query) + _recentSearches.value.filterNot { it == query }).take(RECENT_SEARCH_LIMIT)
        _recentSearches.value = updated
        recentSearchStorage.write(updated)
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val RECENT_SEARCH_LIMIT = 10
    }
}

internal object SearchSavedStateKeys {
    const val LoadedPage = "search.loaded_page"
    const val InputQuery = "search.input_query"
    const val SubmittedQuery = "search.submitted_query"
    const val Sort = "search.sort"
}

internal object SearchInputRules {
    const val MAX_QUERY_LENGTH = 100

    fun limit(value: String): String = TextInputLimit.takeCodePoints(value, MAX_QUERY_LENGTH)

    fun applyChange(current: String, proposed: String): String =
        TextInputLimit.applyChange(current, proposed, MAX_QUERY_LENGTH)
}
