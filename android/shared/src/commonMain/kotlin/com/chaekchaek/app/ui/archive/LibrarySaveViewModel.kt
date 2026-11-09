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
data class RemovedShelfBook(val isbn13: String, val libraryId: Long, val bookId: Long, val savedElsewhere: Boolean)
data class ShelfBookRemovalConfirmation(val book: BookDetailArgs, val shelf: Bookshelf)
enum class ShelfBookAction { Add, Remove }

data class LibrarySaveUiState(
    val pendingBook: BookDetailArgs? = null,
    val pendingShelfId: Long? = null,
    val pendingToggle: Boolean = false,
    val choosingBook: BookDetailArgs? = null,
    val removalConfirmation: ShelfBookRemovalConfirmation? = null,
    val shelves: List<Bookshelf> = emptyList(),
    val selectedShelfId: Long? = null,
    val shelfAction: ShelfBookAction = ShelfBookAction.Add,
    val creatingShelf: Boolean = false,
    val memberId: Long? = null,
    val savingIsbn: String? = null,
    val showLoading: Boolean = false,
    val savedCount: Int = 0,
    val removedCount: Int = 0,
    val lastSavedBook: SavedLibraryBook? = null,
    val failedBook: BookDetailArgs? = null,
    val errorMessage: String? = null,
)

/** 화면 이동 없이 책장 담기·빼기 요청과 인증 대기를 관리한다. 저장 여부는 공통 서재 상태가 소유한다. */
class LibrarySaveViewModel(
    private val addBook: suspend (String, Int?, String) -> LibraryRecord,
    private val onSaved: (BookDetailArgs, LibraryRecord) -> Unit,
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
    private val shelves: BookshelfRepository? = null,
    private val readRecentShelf: (Long) -> Long? = { null },
    private val writeRecentShelf: (Long, Long) -> Unit = { _, _ -> },
    private val isSaved: (String) -> Boolean = { false },
    private val onShelfCreated: () -> Unit = {},
    private val onRemoved: (RemovedShelfBook) -> Unit = {},
) : ViewModel() {
    val errors = ErrorRecoveryState(analytics.errors)
    private val state = MutableStateFlow(LibrarySaveUiState())
    val uiState = state.asStateFlow()
    private val completedSaves = MutableSharedFlow<SavedLibraryBook>(extraBufferCapacity = 1)
    private val completedRemovals = MutableSharedFlow<RemovedShelfBook>(extraBufferCapacity = 1)
    private var accessToken: String? = null
    private var authenticatedMemberId: Long? = null
    private var generation = 0
    private var acknowledgedSavedCount = 0
    private var acknowledgedRemovedCount = 0
    private var saveJob: Job? = null

    /** 각 관찰자가 구독한 뒤 완료된 저장만 전달하며 이전 화면의 완료 상태는 재생하지 않는다. */
    fun savesCompletedWhileObserving(): Flow<SavedLibraryBook> = completedSaves.asSharedFlow()
    fun removalsCompletedWhileObserving(): Flow<RemovedShelfBook> = completedRemovals.asSharedFlow()

    fun authenticate(token: String?, memberId: Long? = null) {
        val memberChanged = authenticatedMemberId != null && memberId != null && authenticatedMemberId != memberId
        if (token == null || memberId != null) authenticatedMemberId = memberId
        if (accessToken == token && !memberChanged) return
        if (!memberChanged && accessToken != null && token != null) {
            accessToken = token
            return
        }
        val pending = state.value.takeIf { accessToken == null && token != null && !memberChanged }
        generation += 1
        errors.dismiss()
        saveJob?.cancel()
        accessToken = token
        state.value = LibrarySaveUiState(memberId = memberId)
        acknowledgedSavedCount = 0
        acknowledgedRemovedCount = 0
        pending?.let { previous -> previous.pendingBook?.let { requestSave(it, previous.pendingShelfId, previous.pendingToggle) } }
    }

    fun consumeSaveNotice(): Boolean {
        val count = state.value.savedCount
        if (count <= acknowledgedSavedCount) return false
        acknowledgedSavedCount = count
        return true
    }

    fun cancelPendingSave() {
        state.value = state.value.copy(pendingBook = null, pendingShelfId = null, pendingToggle = false)
    }

    fun consumeRemovalNotice(): Boolean {
        val count = state.value.removedCount
        if (count <= acknowledgedRemovedCount) return false
        acknowledgedRemovedCount = count
        return true
    }

    fun clearError() {
        state.value = state.value.copy(errorMessage = null, failedBook = null)
    }

    fun save(book: BookDetailArgs) = requestSave(book, null)

    /** 출발 책장을 다시 선택하지 않고 저장한다. 삭제된 책장을 다른 책장으로 대체하지 않는다. */
    fun saveToShelf(book: BookDetailArgs, shelfId: Long) = requestSave(book, shelfId)

    /** 일반 담기 재선택에서는 실제로 담긴 책장만 빼기 대상으로 보여준다. */
    fun toggleSavedBook(book: BookDetailArgs) = requestSave(book, null, toggle = true)

    /** 출발 책장에 바로 담고, 이미 담긴 책은 확인 후 그 책장에서만 뺀다. */
    fun toggleBookInShelf(book: BookDetailArgs, shelfId: Long) = requestSave(book, shelfId, toggle = true)

    private fun requestSave(book: BookDetailArgs, targetShelfId: Long?, toggle: Boolean = false) {
        if ((shelves == null && isSaved(book.isbn13)) || state.value.savingIsbn != null || state.value.pendingBook != null || state.value.choosingBook != null || state.value.removalConfirmation != null) return
        if (book.isbn13.isBlank()) {
            errors.confirm("이 책의 ISBN 정보를 확인할 수 없어요")
            state.value = state.value.copy(errorMessage = "이 책의 ISBN 정보를 확인할 수 없어요")
            return
        }
        val token = accessToken
        if (token == null) {
            state.value = state.value.copy(pendingBook = book, pendingShelfId = targetShelfId, pendingToggle = toggle, errorMessage = null, failedBook = null)
            return
        }
        if (shelves != null) {
            chooseShelf(book, token, targetShelfId, toggle)
            return
        }
        if (targetShelfId != null) {
            errors.confirm("담을 책장 정보를 확인할 수 없어요")
            return
        }
        persist(book, token, null)
    }

    fun selectShelf(id: Long) {
        if (state.value.savingIsbn == null && state.value.shelves.any { it.libraryId == id })
            state.value = state.value.copy(selectedShelfId = id)
    }

    fun dismissShelfChoice() {
        if (state.value.savingIsbn == null) state.value = state.value.copy(choosingBook = null, selectedShelfId = null, creatingShelf = false)
    }

    fun dismissRemovalConfirmation() {
        if (state.value.removalConfirmation == null || state.value.savingIsbn != null) return
        generation += 1
        errors.dismiss()
        state.value = state.value.copy(removalConfirmation = null)
    }

    fun confirmShelfRemoval() {
        val confirmation = state.value.removalConfirmation ?: return
        removeBook(confirmation.book, confirmation.shelf)
    }

    fun beginShelfCreation() {
        if (state.value.choosingBook != null && state.value.savingIsbn == null && state.value.shelfAction == ShelfBookAction.Add)
            state.value = state.value.copy(creatingShelf = true)
    }

    fun cancelShelfCreation() {
        if (state.value.savingIsbn == null) state.value = state.value.copy(creatingShelf = false)
    }

    /** 생성 성공 후에도 담을 책을 유지하며 새 책장을 선택한다. 실제 담기는 사용자가 확정한다. */
    fun createShelf(name: String) {
        val repository = shelves ?: return
        val token = accessToken ?: return
        val book = state.value.choosingBook ?: return
        if (!state.value.creatingShelf || state.value.savingIsbn != null) return
        bookshelfNameError(name, state.value.shelves.map { it.name })?.let { errors.confirm(it); return }
        val requestGeneration = generation
        state.value = state.value.copy(savingIsbn = book.isbn13)
        saveJob = viewModelScope.launch {
            try {
                val created = withDelayedLoading({ loading ->
                    if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                }) { repository.create(name, token) }.getOrThrow()
                if (generation != requestGeneration) return@launch
                state.value = state.value.copy(
                    shelves = state.value.shelves.filterNot { it.libraryId == created.libraryId } + created,
                    selectedShelfId = created.libraryId,
                    creatingShelf = false,
                )
                onShelfCreated()
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                if (generation == requestGeneration) errors.report(e, ErrorOperation.Request) { fresh ->
                    if (fresh != null) accessToken = fresh
                    createShelf(name)
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
        }
    }

    fun confirmShelf() {
        if (state.value.creatingShelf) return
        val book = state.value.choosingBook ?: return
        val shelf = state.value.shelves.firstOrNull { it.libraryId == state.value.selectedShelfId } ?: return
        if (state.value.savingIsbn != null) return
        if (state.value.shelfAction == ShelfBookAction.Remove) {
            removeBook(book, shelf)
            return
        }
        when {
            shelf.items.any { it.isbn13 == book.isbn13 } -> errors.confirm("이미 이 책장에 담긴 책이에요")
            shelf.totalCount >= 30 -> errors.confirm("책장에는 최대 30권까지 담을 수 있어요")
            else -> tokenForSave()?.let { persist(book, it, shelf.libraryId) }
        }
    }

    private fun tokenForSave() = accessToken

    private fun chooseShelf(book: BookDetailArgs, token: String, targetShelfId: Long? = null, toggle: Boolean = false) {
        val requestGeneration = generation
        state.value = state.value.copy(savingIsbn = book.isbn13)
        saveJob = viewModelScope.launch {
            var directShelf: Bookshelf? = null
            var directRemoval: Bookshelf? = null
            try {
                val collection = withDelayedLoading({ loading ->
                    if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                }) { shelves!!.mine(token) }.getOrThrow()
                if (generation != requestGeneration) return@launch
                if (authenticatedMemberId != null && collection.memberId != authenticatedMemberId) {
                    errors.confirm("책장의 계정 정보를 확인할 수 없어요")
                    return@launch
                }
                val savedShelves = collection.libraries.filter { shelf -> shelf.items.any { it.isbn13 == book.isbn13 } }
                state.value = state.value.copy(memberId = collection.memberId, shelves = collection.libraries)
                if (targetShelfId != null) {
                    val target = collection.libraries.firstOrNull { it.libraryId == targetShelfId }
                    when {
                        target == null -> errors.confirm("담으려던 책장을 찾을 수 없어요. 내 서재에서 책장을 확인해 주세요")
                        toggle && target.items.any { it.isbn13 == book.isbn13 } -> directRemoval = target
                        target.items.any { it.isbn13 == book.isbn13 } -> errors.confirm("이미 이 책장에 담긴 책이에요")
                        target.totalCount >= 30 -> errors.confirm("책장에는 최대 30권까지 담을 수 있어요")
                        else -> {
                            state.value = state.value.copy(memberId = collection.memberId)
                            directShelf = target
                        }
                    }
                } else if (toggle && savedShelves.isNotEmpty()) {
                    state.value = state.value.copy(choosingBook = book, shelves = savedShelves,
                        selectedShelfId = savedShelves.first().libraryId, shelfAction = ShelfBookAction.Remove)
                } else if (collection.libraries.isEmpty()) {
                    errors.confirm("기본 책장이 준비되지 않았어요. 잠시 후 다시 시도해 주세요")
                    return@launch
                } else {
                    val recent = readRecentShelf(collection.memberId)
                    val selected = collection.libraries.firstOrNull { it.libraryId == recent }
                        ?: collection.libraries.firstOrNull { it.isDefault }
                        ?: collection.libraries.first()
                    state.value = state.value.copy(choosingBook = book, shelves = collection.libraries,
                        selectedShelfId = selected.libraryId, memberId = collection.memberId, shelfAction = ShelfBookAction.Add)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                if (generation == requestGeneration) errors.report(e, ErrorOperation.LibrarySave) { fresh ->
                    if (generation == requestGeneration) {
                        if (fresh != null) accessToken = fresh
                        requestSave(book, targetShelfId, toggle)
                    }
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
            if (generation == requestGeneration) directShelf?.let { destination ->
                accessToken?.let { persistBook(book, it, destination.libraryId, requestGeneration) }
            }
            if (generation == requestGeneration) directRemoval?.let { target ->
                state.value = state.value.copy(removalConfirmation = ShelfBookRemovalConfirmation(book, target))
            }
        }
    }

    private fun removeBook(book: BookDetailArgs, shelf: Bookshelf) {
        val repository = shelves ?: return
        val token = accessToken ?: return
        if (state.value.savingIsbn != null) return
        val savedBook = shelf.items.firstOrNull { it.isbn13 == book.isbn13 } ?: return
        val requestGeneration = generation
        val savedElsewhere = state.value.shelves.any { it.libraryId != shelf.libraryId && it.items.any { item -> item.isbn13 == book.isbn13 } }
        state.value = state.value.copy(savingIsbn = book.isbn13)
        saveJob = viewModelScope.launch {
            try {
                withDelayedLoading({ if (generation == requestGeneration) state.value = state.value.copy(showLoading = it) }) {
                    repository.remove(shelf.libraryId, savedBook.bookId, token)
                }.getOrThrow()
                if (generation != requestGeneration) return@launch
                val removed = RemovedShelfBook(book.isbn13, shelf.libraryId, savedBook.bookId, savedElsewhere)
                state.value = state.value.copy(choosingBook = null, selectedShelfId = null, removalConfirmation = null,
                    removedCount = state.value.removedCount + 1)
                onRemoved(removed)
                completedRemovals.emit(removed)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) {
                if (generation == requestGeneration) errors.report(e, ErrorOperation.Request) { fresh ->
                    if (generation == requestGeneration) {
                        if (fresh != null) accessToken = fresh
                        removeBook(book, shelf)
                    }
                }
            } finally {
                if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
            }
        }
    }

    private fun persist(book: BookDetailArgs, token: String, shelfId: Long?) {
        val requestGeneration = generation
        state.value = state.value.copy(savingIsbn = book.isbn13, errorMessage = null, failedBook = null)
        saveJob = viewModelScope.launch { persistBook(book, token, shelfId, requestGeneration) }
    }

    private suspend fun persistBook(book: BookDetailArgs, token: String, shelfId: Long?, requestGeneration: Int) {
        if (generation != requestGeneration) return
        val action = analytics.startAction("library_add")
        state.value = state.value.copy(savingIsbn = book.isbn13, errorMessage = null, failedBook = null)
        try {
            val record = withDelayedLoading(
                onLoadingChanged = { loading ->
                    if (generation == requestGeneration) state.value = state.value.copy(showLoading = loading)
                },
            ) {
                if (shelfId != null) shelves!!.add(shelfId, book.isbn13, book.totalPages.takeIf { it > 0 }, token)
                else addBook(book.isbn13, book.totalPages.takeIf { it > 0 }, token)
            }.getOrThrow()
            if (generation != requestGeneration) return
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
                    if (generation == requestGeneration) {
                        if (freshToken != null) accessToken = freshToken
                        when {
                            shelfId == null -> save(book)
                            state.value.choosingBook != null -> confirmShelf()
                            else -> saveToShelf(book, shelfId)
                        }
                    }
                }
                state.value = state.value.copy(failedBook = book, errorMessage = "서재에 담지 못했어요")
            }
        } finally {
            if (generation == requestGeneration) state.value = state.value.copy(savingIsbn = null, showLoading = false)
        }
    }
}
