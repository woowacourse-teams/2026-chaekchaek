package com.chaekchaek.app.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.ui.archive.withDelayedLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookRegistrationUiState(
    val pendingBook: BookSearchResult? = null,
    val registeredBookIds: Set<String> = emptySet(),
    val isBusy: Boolean = false,
    val showLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val loginRequired: Boolean get() = pendingBook != null
}

class BookRegistrationViewModel internal constructor(
    private val addBook: suspend (isbn13: String, totalPages: Int?, accessToken: String) -> Unit,
) : ViewModel() {
    constructor(repository: LibraryRemoteRepository = LibraryRemoteRepository()) : this(repository::add)

    private val _uiState = MutableStateFlow(BookRegistrationUiState())
    val uiState: StateFlow<BookRegistrationUiState> = _uiState.asStateFlow()

    private var accessToken: String? = null
    private var registrationJob: Job? = null

    fun authenticate(accessToken: String?) {
        this.accessToken = accessToken
        if (accessToken != null) resumeRegistration()
    }

    fun register(book: BookSearchResult) {
        val validationError = book.registrationValidationError()
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError)
            return
        }
        val token = accessToken
        if (token == null) {
            _uiState.value = _uiState.value.copy(pendingBook = book, errorMessage = null)
            return
        }
        performRegistration(book, token)
    }

    fun resumeRegistration() {
        val book = _uiState.value.pendingBook ?: return
        val token = accessToken ?: return
        performRegistration(book, token)
    }

    fun cancelRegistration() {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(pendingBook = null, errorMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun performRegistration(book: BookSearchResult, token: String) {
        if (_uiState.value.isBusy) return
        registrationJob?.cancel()
        _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
        registrationJob = viewModelScope.launch {
            withDelayedLoading(::setLoading) {
                addBook(book.isbn13, book.totalPages.takeIf { it > 0 }, token)
            }.onSuccess {
                if (accessToken == token) {
                    _uiState.value = _uiState.value.copy(
                        pendingBook = null,
                        registeredBookIds = _uiState.value.registeredBookIds + book.isbn13,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.value = _uiState.value.copy(errorMessage = "책을 등록하지 못했어요")
            }
            _uiState.value = _uiState.value.copy(isBusy = false)
        }
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(showLoading = loading)
    }
}

internal fun BookSearchResult.registrationValidationError(): String? = when {
    isbn13.isBlank() -> "ISBN 정보가 없는 책은 등록할 수 없어요"
    title.isBlank() -> "책 제목을 입력해 주세요"
    creator.isBlank() -> "저자를 입력해 주세요"
    totalPages < 0 -> "전체 쪽수는 0보다 작을 수 없어요"
    else -> null
}
