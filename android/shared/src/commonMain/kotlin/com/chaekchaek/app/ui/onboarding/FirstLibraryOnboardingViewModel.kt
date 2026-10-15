package com.chaekchaek.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep { INTRO, SEARCH, CHOICE, SAVING, SAVE_ERROR }

data class FirstLibraryOnboardingState(
    val step: OnboardingStep = OnboardingStep.INTRO,
    val query: String = "",
    val results: List<BookSearchResult> = emptyList(),
    val selectedBooks: List<BookSearchResult> = emptyList(),
    val searching: Boolean = false,
    val searchAttempted: Boolean = false,
    val searchError: String? = null,
    val saveError: String? = null,
)

class FirstLibraryOnboardingViewModel(
    private val searchRepository: BookSearchRepository = BookSearchRemoteRepository(),
    private val addBook: suspend (BookSearchResult, String) -> Unit = { book, token ->
        LibraryRemoteRepository().add(book.isbn13, book.totalPages.takeIf { it > 0 }, token)
    },
) : ViewModel() {
    private val _state = MutableStateFlow(FirstLibraryOnboardingState())
    val state: StateFlow<FirstLibraryOnboardingState> = _state.asStateFlow()
    private var job: Job? = null
    private var lastAccessToken: String? = null

    fun startSearch() {
        _state.value = _state.value.copy(step = OnboardingStep.SEARCH)
    }

    fun updateQuery(value: String) {
        _state.value = _state.value.copy(query = value.take(100))
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(searching = true, searchAttempted = true, searchError = null)
            try {
                val page = searchRepository.search(query, BookSearchSort.LATEST, 1)
                _state.value = _state.value.copy(results = page.items, searching = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(searching = false, searchError = "검색을 완료하지 못했어요")
            }
        }
    }

    fun toggle(book: BookSearchResult) {
        val current = _state.value.selectedBooks
        val next = if (current.any { it.isbn13 == book.isbn13 }) {
            current.filterNot { it.isbn13 == book.isbn13 }
        } else {
            current + book
        }
        _state.value = _state.value.copy(selectedBooks = next)
    }

    fun reviewSelection() {
        if (_state.value.selectedBooks.isNotEmpty()) {
            _state.value = _state.value.copy(step = OnboardingStep.CHOICE)
        }
    }

    fun save(accessToken: String, onComplete: () -> Unit) {
        if (_state.value.selectedBooks.isEmpty() || _state.value.step == OnboardingStep.SAVING) return
        lastAccessToken = accessToken
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(step = OnboardingStep.SAVING, saveError = null)
            try {
                _state.value.selectedBooks.forEach { book ->
                    try {
                        addBook(book, accessToken)
                    } catch (error: ClientRequestException) {
                        if (error.response.status != HttpStatusCode.Conflict) throw error
                    }
                }
                onComplete()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    step = OnboardingStep.SAVE_ERROR,
                    saveError = "책을 담지 못했어요. 다시 시도해 주세요.",
                )
            }
        }
    }

    fun retrySave(onComplete: () -> Unit) {
        lastAccessToken?.let { save(it, onComplete) }
    }
}
