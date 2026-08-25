package com.chamsae.chaekchaek.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.toArchivedBook
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
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
  private val isSignedIn: () -> Boolean,
) : ViewModel() {
  private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
  val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
  private val _pendingRegistration = MutableStateFlow<BookSearchResult?>(null)
  val pendingRegistration: StateFlow<BookSearchResult?> = _pendingRegistration.asStateFlow()
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
          val results = bookSearchRepository.search(trimmed).sortedByDescending { it.year.toIntOrNull() ?: Int.MIN_VALUE }
          if (results.isEmpty()) SearchUiState.Empty else SearchUiState.Success(results)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          SearchUiState.Error(e.message ?: "검색 중 오류가 발생했습니다")
        }
    }
  }

  fun register(book: BookSearchResult) {
    if (!isSignedIn()) {
      _pendingRegistration.value = book
      return
    }
    viewModelScope.launch {
      try {
        libraryRepository.add(book.toArchivedBook())
      } catch (error: CancellationException) {
        throw error
      } catch (_: Exception) {
      }
    }
  }

  suspend fun resumeRegistration() {
    val book = _pendingRegistration.value ?: return
    libraryRepository.add(book.toArchivedBook())
    _pendingRegistration.value = null
  }

  fun cancelRegistration() {
    _pendingRegistration.value = null
  }
}
