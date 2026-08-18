package com.chamsae.chaekchaek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chamsae.chaekchaek.data.BookSearchRepository
import com.chamsae.chaekchaek.data.BookSearchResult
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.toArchivedBook
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

enum class SearchSort(val label: String) {
  Latest("최신순"),
  Commented("댓글순"),
}

class SearchViewModel(
  private val bookSearchRepository: BookSearchRepository,
  private val libraryRepository: LibraryRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
  private val _sort = MutableStateFlow(SearchSort.Latest)
  val sort: StateFlow<SearchSort> = _sort.asStateFlow()
  private var searchJob: Job? = null
  private var originalResults = emptyList<BookSearchResult>()

  fun clear() {
    searchJob?.cancel()
    originalResults = emptyList()
    _sort.value = SearchSort.Latest
    _uiState.value = SearchUiState.Idle
  }

  fun search(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    searchJob?.cancel()
    _uiState.value = SearchUiState.Loading
    searchJob = viewModelScope.launch {
      _uiState.value =
        try {
          originalResults = bookSearchRepository.search(trimmed)
          if (originalResults.isEmpty()) {
            SearchUiState.Empty
          } else {
            SearchUiState.Success(sortSearchResults(originalResults, _sort.value))
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다")
        }
    }
  }

  fun selectSort(sort: SearchSort) {
    _sort.value = sort
    if (_uiState.value is SearchUiState.Success) {
      _uiState.value = SearchUiState.Success(sortSearchResults(originalResults, sort))
    }
  }

  fun register(book: BookSearchResult) {
    libraryRepository.add(book.toArchivedBook())
  }
}

internal fun sortSearchResults(
  results: List<BookSearchResult>,
  sort: SearchSort,
): List<BookSearchResult> =
  when (sort) {
    SearchSort.Latest -> results.sortedByDescending { it.year.toIntOrNull() ?: Int.MIN_VALUE }
    // ponytail: 댓글 수가 검색 계약에 추가되면 원본 순서 대신 댓글 수 내림차순으로 교체한다.
    SearchSort.Commented -> results
  }
