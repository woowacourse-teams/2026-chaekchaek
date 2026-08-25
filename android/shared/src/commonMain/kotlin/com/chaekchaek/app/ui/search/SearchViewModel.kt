package com.chaekchaek.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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
    private val registerBook: suspend (BookSearchResult) -> Unit,
    private val isSignedIn: () -> Boolean,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private val _sort = MutableStateFlow(BookSearchSort.LATEST)
    val sort: StateFlow<BookSearchSort> = _sort.asStateFlow()
    private val _pendingRegistration = MutableStateFlow<BookSearchResult?>(null)
    val pendingRegistration: StateFlow<BookSearchResult?> = _pendingRegistration.asStateFlow()
    private var searchJob: Job? = null
    private var currentQuery = ""

    fun clear() {
        searchJob?.cancel()
        currentQuery = ""
        _uiState.value = SearchUiState.Idle
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        currentQuery = trimmed
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = try {
                val page = withDelayedApiLoading(
                    onLoadingChanged = { loading ->
                        if (loading) _uiState.value = SearchUiState.Loading
                    },
                ) {
                    bookSearchRepository.search(trimmed, _sort.value, FIRST_PAGE)
                }
                if (page.items.isEmpty()) SearchUiState.Empty
                else SearchUiState.Success(page.items, page.totalCount, page.nextPage)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
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
        if (currentQuery.isNotEmpty()) search(currentQuery)
    }

    fun register(book: BookSearchResult) {
        if (!isSignedIn()) {
            _pendingRegistration.value = book
            return
        }
        viewModelScope.launch {
            try {
                registerBook(book)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
            }
        }
    }

    fun resumeRegistration() {
        val book = _pendingRegistration.value ?: return
        _pendingRegistration.value = null
        register(book)
    }

    fun cancelRegistration() {
        _pendingRegistration.value = null
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
