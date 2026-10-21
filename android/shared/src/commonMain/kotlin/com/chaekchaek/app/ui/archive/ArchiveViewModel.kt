package com.chaekchaek.app.ui.archive

import com.chaekchaek.app.presentation.common.RefreshUiState
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.ErrorOperation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.chunkLibraryBookIds
import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
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
    val errors = ErrorRecoveryState()
    private val _refreshState = MutableStateFlow(RefreshUiState(enabled = false))
    val refreshState: StateFlow<RefreshUiState> = _refreshState.asStateFlow()
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val mutationMutex = Mutex()
    private var accessToken: String? = null
    private var readJob: Job? = null
    private val mutationJobs = mutableSetOf<Job>()
    private val loadingOperations = mutableSetOf<Int>()
    private var nextOperationId = 0
    private var sessionGeneration = 0
    private var requestGeneration = 0

    fun authenticate(accessToken: String?) {
        if (this.accessToken == accessToken) return
        val renewingMemberSession = this.accessToken != null && accessToken != null
        this.accessToken = accessToken
        invalidateRead()
        if (!renewingMemberSession) {
            sessionGeneration += 1
            mutationJobs.toList().forEach { it.cancel() }
            mutationJobs.clear()
            loadingOperations.clear()
            _uiState.value = ArchiveUiState()
        }
        if (accessToken != null) load()
    }

    fun retry() = load()

    fun refresh() {
        if (readJob?.isActive == true) return
        load(refreshing = true)
    }

    fun acceptSavedBook(book: BookDetailArgs, record: LibraryRecord) {
        if (accessToken == null) return
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
        _uiState.value = _uiState.value.copy(items = _uiState.value.items.filterNot { it.id == isbn13 })
        load()
    }

    fun remove(bookIds: Set<String>) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        val remaining = chunkLibraryBookIds(serverBookIds).toMutableList()
        mutate("library_remove", serverBookIds.size) { token ->
            while (remaining.isNotEmpty()) {
                repository.bulkDelete(remaining.first(), token)
                remaining.removeAt(0)
            }
        }
    }

    fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        val remaining = chunkLibraryBookIds(serverBookIds).toMutableList()
        mutate("reading_status_change", serverBookIds.size) { token ->
            while (remaining.isNotEmpty()) {
                repository.bulkChangeStatus(remaining.first(), status.apiValue, token)
                remaining.removeAt(0)
            }
        }
    }

    private fun load(refreshing: Boolean = false) {
        val token = accessToken ?: return
        invalidateRead()
        val generation = requestGeneration
        val session = sessionGeneration
        _refreshState.value = RefreshUiState(isRefreshing = refreshing)
        readJob = viewModelScope.launch {
            try {
                mutationMutex.withLock {
                    if (session == sessionGeneration && generation == requestGeneration) {
                        loadIntoState(token, generation, session, refreshing)
                    }
                }
            } finally {
                if (session == sessionGeneration && generation == requestGeneration) {
                    _refreshState.value = _refreshState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    private fun invalidateRead() {
        requestGeneration += 1
        readJob?.cancel()
        readJob = null
        _refreshState.value = RefreshUiState(enabled = accessToken != null)
    }

    private fun mutate(
        actionName: String,
        itemCount: Int,
        action: suspend (String) -> Unit,
    ) {
        if (accessToken == null) return
        val analyticsAction = analytics.startAction(actionName, longs = mapOf("item_count" to itemCount.toLong()))
        val session = sessionGeneration
        invalidateRead()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            mutationMutex.withLock {
                if (session != sessionGeneration) return@withLock
                val token = accessToken ?: return@withLock
                withArchiveLoading(session) { action(token) }.onSuccess {
                    analytics.finishAction(analyticsAction, "success")
                    if (session == sessionGeneration) {
                        _uiState.value = _uiState.value.copy(mutationRevision = _uiState.value.mutationRevision + 1)
                        load()
                    }
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    analytics.finishAction(
                        analyticsAction,
                        "failure",
                        strings = mapOf("error_category" to analyticsErrorCategory(error)),
                    )
                    if (session == sessionGeneration) {
                        errors.report(error, ErrorOperation.Request) { freshToken ->
                            if (freshToken != null) accessToken = freshToken
                            mutate(actionName, itemCount, action)
                        }
                    }
                }
            }
        }
        mutationJobs += job
        job.invokeOnCompletion { mutationJobs -= job }
        job.start()
    }

    private suspend fun loadIntoState(token: String, generation: Int, session: Int, refreshing: Boolean) {
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        withArchiveLoading(session, showLoading = !refreshing) { repository.getAll(token) }
            .onSuccess { books ->
                analytics.log(
                    name = "cc_load_result",
                    strings = mapOf("resource" to "library", "outcome" to if (books.isEmpty()) "empty" else "success"),
                    longs = mapOf(
                        "duration_ms" to started.elapsedNow().inWholeMilliseconds,
                        "result_count" to books.size.toLong(),
                    ),
                )
                if (session == sessionGeneration && generation == requestGeneration) {
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
                if (session == sessionGeneration && generation == requestGeneration) handleFailure(error, refreshing)
            }
    }

    private fun handleFailure(error: Throwable, refreshing: Boolean) {
        if (error is CancellationException) throw error
        errors.report(error, ErrorOperation.LibraryLoad) { freshToken ->
            if (freshToken != null) accessToken = freshToken
            load(refreshing = refreshing)
        }
        _uiState.value = _uiState.value.copy(errorMessage = "서재를 불러오지 못했어요")
    }

    private suspend fun <T> withArchiveLoading(session: Int, showLoading: Boolean = true, action: suspend () -> T): Result<T> {
        val operationId = ++nextOperationId
        fun setLoading(loading: Boolean) {
            if (session != sessionGeneration || !showLoading) return
            if (loading) loadingOperations += operationId else loadingOperations -= operationId
            _uiState.value = _uiState.value.copy(showLoading = loadingOperations.isNotEmpty())
        }
        return try {
            withDelayedLoading(::setLoading, action)
        } finally {
            setLoading(false)
        }
    }
}
