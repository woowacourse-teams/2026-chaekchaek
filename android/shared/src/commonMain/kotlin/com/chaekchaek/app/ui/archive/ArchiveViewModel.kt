package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ArchiveViewModel(
    private val repository: LibraryRemoteRepository,
    private val analytics: AnalyticsTracker = AnalyticsTracker.None,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val mutationMutex = Mutex()
    private var accessToken: String? = null
    private var libraryJob: Job? = null
    private var requestGeneration = 0

    fun authenticate(accessToken: String?) {
        if (this.accessToken == accessToken) return
        this.accessToken = accessToken
        requestGeneration += 1
        libraryJob?.cancel()
        _uiState.value = ArchiveUiState()
        if (accessToken != null) load()
    }

    fun retry() = load()

    fun acceptSavedBook(book: BookDetailArgs, record: LibraryRecord) {
        if (accessToken == null) return
        libraryJob?.cancel()
        val bookId = record.bookId ?: book.bookId
        if (bookId != null) {
            val existing = _uiState.value.items.firstOrNull { it.id == book.isbn13 }
            val saved = ArchiveBookUiModel(
                id = book.isbn13,
                bookId = bookId,
                title = existing?.title ?: book.title,
                creator = existing?.creator ?: book.creator,
                publisher = existing?.publisher ?: book.publisher,
                category = existing?.category ?: book.category,
                coverUrl = existing?.coverUrl ?: book.coverUrl.ifBlank { book.coverId },
                spineUrl = existing?.spineUrl,
                status = ReadingStatus.entries.firstOrNull { it.apiValue == record.status } ?: ReadingStatus.READING,
                currentPage = record.currentPage,
                totalPages = existing?.totalPages ?: book.totalPages,
                lastRecordedAt = existing?.lastRecordedAt ?: 0L,
            )
            _uiState.value = _uiState.value.copy(
                items = _uiState.value.items.filterNot { it.id == book.isbn13 } + saved,
                errorMessage = null,
            )
        }
        load()
    }

    fun acceptRemovedBook(isbn13: String) {
        if (accessToken == null) return
        libraryJob?.cancel()
        _uiState.value = _uiState.value.copy(items = _uiState.value.items.filterNot { it.id == isbn13 })
        load()
    }

    fun remove(bookIds: Set<String>) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        mutate("library_remove", serverBookIds.size) { token -> repository.bulkDelete(serverBookIds, token) }
    }

    fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        mutate("reading_status_change", serverBookIds.size) { token ->
            repository.bulkChangeStatus(serverBookIds, status.apiValue, token)
        }
    }

    private fun load() {
        val token = accessToken ?: return
        val generation = ++requestGeneration
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch { loadIntoState(token, generation) }
    }

    private fun mutate(
        actionName: String,
        itemCount: Int,
        action: suspend (String) -> Unit,
    ) {
        val token = accessToken ?: return
        val analyticsAction = analytics.startAction(actionName, longs = mapOf("item_count" to itemCount.toLong()))
        val generation = ++requestGeneration
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            mutationMutex.withLock {
                withDelayedLoading({ if (generation == requestGeneration) setLoading(it) }) {
                    action(token)
                    repository.getAll(token)
                }.onSuccess { books ->
                    analytics.finishAction(analyticsAction, "success")
                    if (accessToken == token && generation == requestGeneration) {
                        _uiState.value = _uiState.value.copy(
                            items = books.map { it.toArchiveBookUiModel() },
                            errorMessage = null,
                        )
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    analytics.finishAction(
                        analyticsAction,
                        "failure",
                        strings = mapOf("error_category" to analyticsErrorCategory(error)),
                    )
                    if (generation == requestGeneration) handleFailure(error)
                }
            }
        }
    }

    private suspend fun loadIntoState(token: String, generation: Int) {
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        withDelayedLoading({ if (generation == requestGeneration) setLoading(it) }) { repository.getAll(token) }
            .onSuccess { books ->
                analytics.log(
                    name = "cc_load_result",
                    strings = mapOf("resource" to "library", "outcome" to if (books.isEmpty()) "empty" else "success"),
                    longs = mapOf(
                        "duration_ms" to started.elapsedNow().inWholeMilliseconds,
                        "result_count" to books.size.toLong(),
                    ),
                )
                if (accessToken == token && generation == requestGeneration) {
                    _uiState.value = _uiState.value.copy(
                        items = books.map { it.toArchiveBookUiModel() },
                        errorMessage = null,
                    )
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                analytics.log(
                    name = "cc_load_result",
                    strings = mapOf(
                        "resource" to "library",
                        "outcome" to "failure",
                        "error_category" to analyticsErrorCategory(error),
                    ),
                    longs = mapOf("duration_ms" to started.elapsedNow().inWholeMilliseconds),
                )
                if (generation == requestGeneration) handleFailure(error)
            }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(errorMessage = "서재를 불러오지 못했어요")
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(showLoading = loading)
    }
}
