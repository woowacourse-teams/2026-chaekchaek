package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibrarySaveUiState(
    val pendingBook: BookDetailArgs? = null,
    val savingIsbn: String? = null,
    val showLoading: Boolean = false,
    val savedCount: Int = 0,
    val failedBook: BookDetailArgs? = null,
    val errorMessage: String? = null,
)

/** 화면 이동 없이 책만 담는 요청과 인증 대기를 관리한다. 저장 여부는 공통 서재 상태가 소유한다. */
class LibrarySaveViewModel(
    private val addBook: suspend (String, Int?, String) -> LibraryRecord,
    private val onSaved: (BookDetailArgs, LibraryRecord) -> Unit,
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
    private val isSaved: (String) -> Boolean = { false },
) : ViewModel() {
    private val state = MutableStateFlow(LibrarySaveUiState())
    val uiState = state.asStateFlow()
    private var accessToken: String? = null
    private var generation = 0
    private var acknowledgedSavedCount = 0
    private var saveJob: Job? = null

    fun authenticate(token: String?) {
        if (accessToken == token) return
        val pending = state.value.pendingBook.takeIf { accessToken == null && token != null }
        generation += 1
        saveJob?.cancel()
        accessToken = token
        state.value = LibrarySaveUiState()
        acknowledgedSavedCount = 0
        pending?.let(::save)
    }

    fun consumeSaveNotice(): Boolean {
        val count = state.value.savedCount
        if (count <= acknowledgedSavedCount) return false
        acknowledgedSavedCount = count
        return true
    }

    fun cancelPendingSave() {
        state.value = state.value.copy(pendingBook = null)
    }

    fun clearError() {
        state.value = state.value.copy(errorMessage = null, failedBook = null)
    }

    fun save(book: BookDetailArgs) {
        if (isSaved(book.isbn13) || state.value.savingIsbn != null || state.value.pendingBook != null) return
        if (book.isbn13.isBlank()) {
            state.value = state.value.copy(errorMessage = "이 책의 ISBN 정보를 확인할 수 없어요")
            return
        }
        val token = accessToken
        if (token == null) {
            state.value = state.value.copy(pendingBook = book, errorMessage = null, failedBook = null)
            return
        }
        val requestGeneration = generation
        val action = analytics.startAction("library_add")
        state.value = state.value.copy(savingIsbn = book.isbn13, errorMessage = null, failedBook = null)
        saveJob = viewModelScope.launch {
            try {
                val record = withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                    },
                ) { addBook(book.isbn13, book.totalPages.takeIf { it > 0 }, token) }.getOrThrow()
                if (generation != requestGeneration) return@launch
                onSaved(book, record)
                state.value = state.value.copy(savedCount = state.value.savedCount + 1)
                analytics.finishAction(action, "success")
            } catch (error: CancellationException) {
                analytics.finishAction(action, "cancelled")
                throw error
            } catch (error: Throwable) {
                analytics.finishAction(action, "failure", strings = mapOf("error_category" to analyticsErrorCategory(error)))
                if (generation == requestGeneration) {
                    state.value = state.value.copy(failedBook = book, errorMessage = "서재에 담지 못했어요")
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
        }
    }
}
