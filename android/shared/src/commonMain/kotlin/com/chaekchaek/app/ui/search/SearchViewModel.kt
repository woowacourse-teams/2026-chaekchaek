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
import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.presentation.common.RefreshUiState
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
        val showLoadingMore: Boolean = false,
        val loadMoreFailed: Boolean = false,
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
    private var loadMoreJob: Job? = null
    private var loadRevision = 0L
    private val _refreshState = MutableStateFlow(RefreshUiState())
    val refreshState: StateFlow<RefreshUiState> = _refreshState.asStateFlow()
    private var currentQuery = SearchInputRules.limit(
        savedStateHandle.get<String>(SearchSavedStateKeys.SubmittedQuery).orEmpty(),
    )
    private var registerBook = registerBook
    private var isSignedIn = isSignedIn

    init {
        savedStateHandle[SearchSavedStateKeys.InputQuery] = _query.value
        savedStateHandle[SearchSavedStateKeys.SubmittedQuery] = currentQuery
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
        loadRevision += 1
        searchJob?.cancel()
        loadMoreJob?.cancel()
        _refreshState.value = RefreshUiState()
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

    fun refresh() {
        if (currentQuery.isEmpty() || searchJob?.isActive == true) return
        search(currentQuery, trigger = "refresh", trackAnalytics = false, retainContent = true)
    }

    private fun search(
        query: String,
        trigger: String,
        trackAnalytics: Boolean = true,
        retainContent: Boolean = false,
    ) {
        val trimmed = SearchInputRules.limit(query.trim())
        if (trimmed.isEmpty()) return
        val restoreThroughPage = if (trigger == "restore" || retainContent) {
            savedStateHandle.get<Int>(SearchSavedStateKeys.LoadedPage) ?: FIRST_PAGE
        } else FIRST_PAGE
        if (trigger != "restore" && !retainContent) savedStateHandle[SearchSavedStateKeys.LoadedPage] = FIRST_PAGE
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
        val revision = ++loadRevision
        val sort = _sort.value
        searchJob?.cancel()
        loadMoreJob?.cancel()
        val previousState = (_uiState.value as? SearchUiState.Success)?.copy(
            loadingMore = false, showLoadingMore = false,
        ) ?: _uiState.value
        if (retainContent) _uiState.value = previousState
        _refreshState.value = RefreshUiState(isRefreshing = retainContent)
        searchJob = viewModelScope.launch {
            val result = try {
                val page = withDelayedApiLoading(
                    onLoadingChanged = { loading ->
                        if (revision == loadRevision) {
                            if (retainContent) _refreshState.value = _refreshState.value.copy(showLoading = loading)
                            else if (loading) _uiState.value = SearchUiState.Loading
                        }
                    },
                ) {
                    var page = bookSearchRepository.search(trimmed, sort, FIRST_PAGE)
                    var loadedPage = FIRST_PAGE
                    while (true) {
                        val nextPage = page.nextPage?.takeIf { it > loadedPage && it <= restoreThroughPage }
                            ?: break
                        val next = bookSearchRepository.search(trimmed, sort, nextPage)
                        page = next.copy(items = (page.items + next.items).distinctBy(BookSearchResult::isbn13))
                        loadedPage = nextPage
                    }
                    if (revision == loadRevision) savedStateHandle[SearchSavedStateKeys.LoadedPage] = loadedPage
                    page
                }
                if (revision != loadRevision) return@launch
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
                if (revision != loadRevision) return@launch
                if (retainContent) {
                    _refreshState.value = _refreshState.value.copy(errorMessage = "새로고침하지 못했어요")
                    previousState
                } else SearchUiState.Error(error.message ?: "검색 중 오류가 발생했습니다")
            }
            if (revision == loadRevision) {
                _uiState.value = result
                _refreshState.value = _refreshState.value.copy(isRefreshing = false, showLoading = false)
            }
        }
    }

    fun loadMore() {
        val current = _uiState.value as? SearchUiState.Success ?: return
        val page = current.nextPage ?: return
        if (current.loadingMore || current.loadMoreFailed || searchJob?.isActive == true || loadMoreJob?.isActive == true) return
        val revision = loadRevision
        val query = currentQuery
        val sort = _sort.value
        _uiState.value = current.copy(loadingMore = true)
        loadMoreJob = viewModelScope.launch {
            try {
                val next = withDelayedApiLoading(onLoadingChanged = { loading ->
                    if (revision == loadRevision) {
                        (_uiState.value as? SearchUiState.Success)?.let { _uiState.value = it.copy(showLoadingMore = loading) }
                    }
                }) { bookSearchRepository.search(query, sort, page) }
                if (revision != loadRevision) return@launch
                val latest = _uiState.value as? SearchUiState.Success ?: return@launch
                if (currentQuery != query || _sort.value != sort || latest.nextPage != page) return@launch
                savedStateHandle[SearchSavedStateKeys.LoadedPage] = page
                _uiState.value = latest.copy(
                    results = (latest.results + next.items).distinctBy(BookSearchResult::isbn13),
                    totalCount = next.totalCount,
                    nextPage = next.nextPage,
                    loadingMore = false,
                    showLoadingMore = false,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (revision != loadRevision) return@launch
                val latest = _uiState.value as? SearchUiState.Success ?: return@launch
                if (latest.nextPage == page) _uiState.value = latest.copy(loadingMore = false, showLoadingMore = false, loadMoreFailed = true)
            }
        }
    }

    fun retryLoadMore() {
        val current = _uiState.value as? SearchUiState.Success ?: return
        if (!current.loadMoreFailed) return
        _uiState.value = current.copy(loadMoreFailed = false)
        loadMore()
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

    val Policy = TextInputPolicy(MAX_QUERY_LENGTH)

    fun limit(value: String): String = Policy.limit(value)

    fun applyChange(current: String, proposed: String): String =
        Policy.applyChange(current, proposed)
}
