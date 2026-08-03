package com.chamsae.chaekchaek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chamsae.chaekchaek.data.BookSearchApi
import com.chamsae.chaekchaek.data.BookSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
  data object Idle : SearchUiState

  data object Loading : SearchUiState

  data class Success(val results: List<BookSearchResult>) : SearchUiState

  data class Error(val message: String) : SearchUiState
}

class SearchViewModel : ViewModel() {
  private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
  private var searchJob: Job? = null

  fun search(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    searchJob?.cancel()
    _uiState.value = SearchUiState.Loading
    searchJob = viewModelScope.launch {
      _uiState.value =
        try {
          SearchUiState.Success(BookSearchApi.search(trimmed))
        } catch (e: Exception) {
          SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다")
        }
    }
  }
}
