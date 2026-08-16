package com.chaekchaek.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.BookSearchApi
import com.chaekchaek.app.data.BookSearchResult
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
  private val searchBooks: suspend (String) -> List<BookSearchResult> = BookSearchApi::search,
) : ViewModel() {
  private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
  private var searchJob: Job? = null

  fun clear() {
    searchJob?.cancel()
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
          val results = searchBooks(trimmed).sortedByDescending { it.year.toIntOrNull() ?: Int.MIN_VALUE }
          if (results.isEmpty()) SearchUiState.Empty else SearchUiState.Success(results)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다")
        }
    }
  }
}
