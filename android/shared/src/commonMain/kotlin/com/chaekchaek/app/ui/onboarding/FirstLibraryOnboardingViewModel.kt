package com.chaekchaek.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import com.chaekchaek.app.ui.search.SearchInputRules
import com.chaekchaek.app.domain.book.BookSearchSort
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep { INTRO, SEARCH, CHOICE, LOGIN_ERROR, SAVING, SAVE_ERROR }

data class FirstLibraryOnboardingState(
    val visible: Boolean = true,
    val step: OnboardingStep = OnboardingStep.INTRO,
    val stepRevision: Int = 0,
    val query: String = "",
    val results: List<BookSearchResult> = emptyList(),
    val selectedBooks: List<BookSearchResult> = emptyList(),
    val searching: Boolean = false,
    val showSearchLoading: Boolean = false,
    val showSaveLoading: Boolean = false,
    val searchAttempted: Boolean = false,
    val searchError: String? = null,
    val loginError: String? = null,
    val saveError: String? = null,
    val saveCompleted: Boolean = false,
)

class FirstLibraryOnboardingViewModel internal constructor(
    private val searchRepository: BookSearchRepository,
    private val addBook: suspend (BookSearchResult, String) -> Unit,
    initiallyVisible: Boolean = true,
) : ViewModel() {
    constructor(
        searchRepository: BookSearchRepository = BookSearchRemoteRepository(),
        libraryRepository: LibraryRemoteRepository = LibraryRemoteRepository(),
        initiallyVisible: Boolean = true,
    ) : this(
        searchRepository = searchRepository,
        initiallyVisible = initiallyVisible,
        addBook = { book, token ->
            libraryRepository.add(book.isbn13, book.totalPages.takeIf { it > 0 }, token)
        },
    )

    private val _state = MutableStateFlow(FirstLibraryOnboardingState(visible = initiallyVisible))
    val state: StateFlow<FirstLibraryOnboardingState> = _state.asStateFlow()
    private var job: Job? = null
    private val savedBookIsbn13 = mutableSetOf<String>()

    fun startSearch() {
        _state.value = _state.value.copy(step = OnboardingStep.SEARCH, stepRevision = _state.value.stepRevision + 1)
    }

    fun updateQuery(value: String) {
        _state.value = _state.value.copy(query = SearchInputRules.applyChange(_state.value.query, value))
    }

    fun search() {
        if (_state.value.step != OnboardingStep.SEARCH) return
        val query = _state.value.query.trim()
        if (query.isEmpty()) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(searching = true, searchAttempted = true, searchError = null)
            try {
                val page = withDelayedApiLoading(
                    onLoadingChanged = { visible -> _state.value = _state.value.copy(showSearchLoading = visible) },
                ) { searchRepository.search(query, BookSearchSort.LATEST, 1) }
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
            _state.value = _state.value.copy(step = OnboardingStep.CHOICE, stepRevision = _state.value.stepRevision + 1)
        }
    }

    fun returnToPreviousStep(
        expectedStep: OnboardingStep = _state.value.step,
        expectedRevision: Int = _state.value.stepRevision,
    ) {
        if (_state.value.step != expectedStep || _state.value.stepRevision != expectedRevision) return
        val previous = when (_state.value.step) {
            OnboardingStep.SEARCH -> OnboardingStep.INTRO
            OnboardingStep.CHOICE, OnboardingStep.LOGIN_ERROR -> OnboardingStep.SEARCH
            OnboardingStep.SAVE_ERROR -> OnboardingStep.CHOICE
            OnboardingStep.INTRO, OnboardingStep.SAVING -> return
        }
        _state.value = _state.value.copy(step = previous, stepRevision = _state.value.stepRevision + 1, loginError = null, saveError = null)
    }

    fun showLoginError(message: String) {
        _state.value = _state.value.copy(step = OnboardingStep.LOGIN_ERROR, stepRevision = _state.value.stepRevision + 1, loginError = message)
    }

    fun save(accessToken: String) {
        if (_state.value.selectedBooks.isEmpty() || _state.value.step == OnboardingStep.SAVING) return
        job?.cancel()
        _state.value = _state.value.copy(
            step = OnboardingStep.SAVING, stepRevision = _state.value.stepRevision + 1, saveError = null,
            searching = false, showSearchLoading = false,
        )
        job = viewModelScope.launch {
            try {
                withDelayedApiLoading(
                    onLoadingChanged = { visible -> _state.value = _state.value.copy(showSaveLoading = visible) },
                ) {
                    _state.value.selectedBooks
                        .filterNot { it.isbn13 in savedBookIsbn13 }
                        .forEach { book ->
                            try {
                                addBook(book, accessToken)
                            } catch (error: ClientRequestException) {
                                if (error.response.status != HttpStatusCode.Conflict) throw error
                            }
                            savedBookIsbn13 += book.isbn13
                        }
                }
                _state.value = _state.value.copy(saveCompleted = true)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    step = OnboardingStep.SAVE_ERROR,
                    stepRevision = _state.value.stepRevision + 1,
                    saveError = "책을 담지 못했어요. 다시 시도해 주세요.",
                )
            }
        }
    }

    fun retrySave(accessToken: String) {
        save(accessToken)
    }

    fun finish() {
        _state.value = _state.value.copy(visible = false)
    }

    fun cancelPendingWork() {
        job?.cancel()
        job = null
        _state.value = _state.value.copy(searching = false, showSearchLoading = false, showSaveLoading = false)
    }
}
