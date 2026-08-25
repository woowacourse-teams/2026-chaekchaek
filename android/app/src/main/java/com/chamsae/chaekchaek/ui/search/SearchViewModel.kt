package com.chamsae.chaekchaek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.toArchivedBook
import com.chamsae.chaekchaek.ui.common.withDelayedApiLoading
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
  data object Idle : SearchUiState

  data object Loading : SearchUiState

  data object Empty : SearchUiState

  data class Success(val results: List<BookSearchResult>) : SearchUiState

  data class Error(val message: String) : SearchUiState
}

class SearchViewModel(
  private val bookSearchRepository: BookSearchRepository,
  private val libraryRepository: LibraryRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
  private val _sort = MutableStateFlow(BookSearchSort.LATEST)
  val sort: StateFlow<BookSearchSort> = _sort.asStateFlow()
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
      _uiState.value =
        try {
          val results =
            withDelayedApiLoading(
              onLoadingChanged = { loading -> if (loading) _uiState.value = SearchUiState.Loading },
            ) {
              bookSearchRepository.search(trimmed, _sort.value)
            }
          if (results.isEmpty()) SearchUiState.Empty else SearchUiState.Success(results)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다")
        }
    }
  }

  fun selectSort(sort: BookSearchSort) {
    if (_sort.value == sort) return
    _sort.value = sort
    if (currentQuery.isNotEmpty()) search(currentQuery)
  }

  fun register(book: BookSearchResult) {
    viewModelScope.launch {
      try {
        libraryRepository.add(book.toArchivedBook())
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
      }
    }
  }
}
