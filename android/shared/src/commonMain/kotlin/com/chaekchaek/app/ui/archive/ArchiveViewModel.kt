package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsErrorCategory
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.presentation.common.RefreshUiState
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
    private var queryJob: Job? = null
    private val mutationJobs = mutableSetOf<Job>()
    private var authenticationRevision = 0L
    private var queryRevision = 0L
    private val _refreshState = MutableStateFlow(RefreshUiState(enabled = false))
    val refreshState: StateFlow<RefreshUiState> = _refreshState.asStateFlow()

    fun authenticate(accessToken: String?) {
        if (this.accessToken == accessToken) return
        this.accessToken = accessToken
        authenticationRevision += 1
        mutationJobs.toList().forEach { it.cancel() }
        mutationJobs.clear()
        queryRevision += 1
        queryJob?.cancel()
        _refreshState.value = RefreshUiState(enabled = accessToken != null)
        if (accessToken == null) {
            _uiState.value = _uiState.value.copy(items = emptyList(), showLoading = false, errorMessage = null)
        } else {
            load()
        }
    }

    fun retry() = load()

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

    fun refresh() {
        if (queryJob?.isActive == true) return
        load(refreshing = true)
    }

    private fun load(refreshing: Boolean = false) {
        val token = accessToken ?: return
        val authentication = authenticationRevision
        val revision = ++queryRevision
        queryJob?.cancel()
        _refreshState.value = RefreshUiState(isRefreshing = refreshing)
        queryJob = viewModelScope.launch {
            // 조회는 쓰기가 끝난 뒤 실행한다. 당김으로 쓰기 요청을 취소하지 않는다.
            mutationMutex.withLock {
                if (authentication != authenticationRevision || revision != queryRevision) return@withLock
                loadIntoState(token, authentication, revision, refreshing)
            }
            if (revision == queryRevision && authentication == authenticationRevision) {
                _refreshState.value = _refreshState.value.copy(isRefreshing = false, showLoading = false)
            }
        }
    }

    private fun mutate(
        actionName: String,
        itemCount: Int,
        action: suspend (String) -> Unit,
    ) {
        val token = accessToken ?: return
        val analyticsAction = analytics.startAction(actionName, longs = mapOf("item_count" to itemCount.toLong()))
        val authentication = authenticationRevision
        queryRevision += 1
        queryJob?.cancel()
        _refreshState.value = RefreshUiState()
        val job = viewModelScope.launch {
            mutationMutex.withLock {
                if (authentication != authenticationRevision) return@withLock
                withDelayedLoading({ loading ->
                    if (authentication == authenticationRevision) setLoading(loading)
                }) {
                    action(token)
                    repository.getAll(token)
                }.onSuccess { books ->
                    analytics.finishAction(analyticsAction, "success")
                    if (authentication == authenticationRevision) {
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
                    if (authentication == authenticationRevision) handleFailure(error)
                }
            }
        }
        mutationJobs += job
        job.invokeOnCompletion { mutationJobs -= job }
    }

    private suspend fun loadIntoState(token: String, authentication: Long, revision: Long, refreshing: Boolean) {
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        withDelayedLoading({ loading ->
            if (authentication == authenticationRevision && revision == queryRevision) {
                if (refreshing) _refreshState.value = _refreshState.value.copy(showLoading = loading)
                else setLoading(loading)
            }
        }) { repository.getAll(token) }
            .onSuccess { books ->
                analytics.log(
                    name = "cc_load_result",
                    strings = mapOf("resource" to "library", "outcome" to if (books.isEmpty()) "empty" else "success"),
                    longs = mapOf(
                        "duration_ms" to started.elapsedNow().inWholeMilliseconds,
                        "result_count" to books.size.toLong(),
                    ),
                )
                if (authentication == authenticationRevision && revision == queryRevision) {
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
                if (authentication == authenticationRevision && revision == queryRevision) {
                    if (refreshing) _refreshState.value = _refreshState.value.copy(errorMessage = "새로고침하지 못했어요")
                    else handleFailure(error)
                }
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
