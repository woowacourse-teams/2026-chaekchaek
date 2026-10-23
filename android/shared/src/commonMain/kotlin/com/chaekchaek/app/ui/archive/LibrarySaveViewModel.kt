package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.ErrorOperation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.Bookshelf
import com.chaekchaek.app.data.remote.BookshelfRepository
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SavedLibraryBook(val isbn13: String, val record: LibraryRecord)

data class LibrarySaveUiState(
    val pendingBook: BookDetailArgs? = null,
    val choosingBook: BookDetailArgs? = null,
    val shelves: List<Bookshelf> = emptyList(),
    val selectedShelfId: Long? = null,
    val memberId: Long? = null,
    val savingIsbn: String? = null,
    val showLoading: Boolean = false,
    val savedCount: Int = 0,
    val lastSavedBook: SavedLibraryBook? = null,
    val failedBook: BookDetailArgs? = null,
    val errorMessage: String? = null,
)

/** 화면 이동 없이 책만 담는 요청과 인증 대기를 관리한다. 저장 여부는 공통 서재 상태가 소유한다. */
class LibrarySaveViewModel(
    private val addBook: suspend (String, Int?, String) -> LibraryRecord,
    private val onSaved: (BookDetailArgs, LibraryRecord) -> Unit,
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
    private val shelves: BookshelfRepository? = null,
    private val readRecentShelf: (Long) -> Long? = { null },
    private val writeRecentShelf: (Long, Long) -> Unit = { _, _ -> },
    private val isSaved: (String) -> Boolean = { false },
) : ViewModel() {
    val errors = ErrorRecoveryState(analytics.errors)
    private val state = MutableStateFlow(LibrarySaveUiState())
    val uiState = state.asStateFlow()
    private val completedSaves = MutableSharedFlow<SavedLibraryBook>(extraBufferCapacity = 1)
    private var accessToken: String? = null
    private var generation = 0
    private var acknowledgedSavedCount = 0
    private var saveJob: Job? = null

    /** 각 관찰자가 구독한 뒤 완료된 저장만 전달하며 이전 화면의 완료 상태는 재생하지 않는다. */
    fun savesCompletedWhileObserving(): Flow<SavedLibraryBook> = completedSaves.asSharedFlow()

    fun authenticate(token: String?) {
        if (accessToken == token) return
        if (accessToken != null && token != null) {
            accessToken = token
            return
        }
        val pending = state.value.pendingBook.takeIf { accessToken == null && token != null }
        generation += 1
        errors.dismiss()
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
        if ((shelves == null && isSaved(book.isbn13)) || state.value.savingIsbn != null || state.value.pendingBook != null || state.value.choosingBook != null) return
        if (book.isbn13.isBlank()) {
            errors.confirm("이 책의 ISBN 정보를 확인할 수 없어요")
            state.value = state.value.copy(errorMessage = "이 책의 ISBN 정보를 확인할 수 없어요")
            return
        }
        val token = accessToken
        if (token == null) {
            state.value = state.value.copy(pendingBook = book, errorMessage = null, failedBook = null)
            return
        }
        if (shelves != null) {
            chooseShelf(book, token)
            return
        }
        persist(book, token, null)
    }

    fun selectShelf(id: Long) {
        if (state.value.savingIsbn == null && state.value.shelves.any { it.libraryId == id })
            state.value = state.value.copy(selectedShelfId = id)
    }

    fun dismissShelfChoice() {
        if (state.value.savingIsbn == null) state.value = state.value.copy(choosingBook = null, selectedShelfId = null)
    }

    fun confirmShelf() {
        val book = state.value.choosingBook ?: return
        val shelf = state.value.shelves.firstOrNull { it.libraryId == state.value.selectedShelfId } ?: return
        if (state.value.savingIsbn != null) return
        when {
            shelf.items.any { it.isbn13 == book.isbn13 } -> errors.confirm("이미 이 책장에 담긴 책이에요")
            shelf.totalCount >= 30 -> errors.confirm("책장에는 최대 30권까지 담을 수 있어요")
            else -> tokenForSave()?.let { persist(book, it, shelf.libraryId) }
        }
    }

    private fun tokenForSave() = accessToken

    private fun chooseShelf(book: BookDetailArgs, token: String) {
        val requestGeneration = generation
        state.value = state.value.copy(savingIsbn = book.isbn13)
        saveJob = viewModelScope.launch {
            try {
                val collection = withDelayedLoading({ loading ->
                    if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                }) { shelves!!.mine(token) }.getOrThrow()
                if (generation != requestGeneration) return@launch
                if (collection.libraries.isEmpty()) {
                    errors.confirm("기본 책장이 준비되지 않았어요. 잠시 후 다시 시도해 주세요")
                    return@launch
                }
                val recent = readRecentShelf(collection.memberId)
                val selected = collection.libraries.firstOrNull { it.libraryId == recent }
                    ?: collection.libraries.firstOrNull { it.isDefault }
                    ?: collection.libraries.first()
                state.value = state.value.copy(choosingBook = book, shelves = collection.libraries,
                    selectedShelfId = selected.libraryId, memberId = collection.memberId)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                if (generation == requestGeneration) errors.report(e, ErrorOperation.LibrarySave) { fresh ->
                    if (fresh != null) accessToken = fresh
                    save(book)
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
        }
    }

    private fun persist(book: BookDetailArgs, token: String, shelfId: Long?) {
        val requestGeneration = generation
        val action = analytics.startAction("library_add")
        state.value = state.value.copy(savingIsbn = book.isbn13, errorMessage = null, failedBook = null)
        saveJob = viewModelScope.launch {
            try {
                val record = withDelayedLoading(
                    onLoadingChanged = { loading ->
                        if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                    },
                ) {
                    if (shelfId != null) shelves!!.add(shelfId, book.isbn13, book.totalPages.takeIf { it > 0 }, token)
                    else addBook(book.isbn13, book.totalPages.takeIf { it > 0 }, token)
                }.getOrThrow()
                if (generation != requestGeneration) return@launch
                if (shelfId != null) state.value.memberId?.let { writeRecentShelf(it, shelfId) }
                onSaved(book, record)
                val savedBook = SavedLibraryBook(book.isbn13, record)
                state.value = state.value.copy(
                    choosingBook = null,
                    selectedShelfId = null,
                    savedCount = state.value.savedCount + 1,
                    lastSavedBook = savedBook,
                )
                completedSaves.emit(savedBook)
                analytics.finishAction(action, "success")
            } catch (error: CancellationException) {
                analytics.finishAction(action, "cancelled")
                throw error
            } catch (error: Throwable) {
                analytics.finishAction(action, "failure", strings = mapOf("error_category" to analyticsErrorCategory(error)))
                if (generation == requestGeneration) {
                    errors.report(error, ErrorOperation.LibrarySave) { freshToken ->
                        if (freshToken != null) accessToken = freshToken
                        if (shelfId == null) save(book) else confirmShelf()
                    }
                    state.value = state.value.copy(failedBook = book, errorMessage = "서재에 담지 못했어요")
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
        }
    }
}
