package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    private val mutationMutex = Mutex()
    private var accessToken: String? = null
    private var libraryJob: Job? = null

    fun authenticate(accessToken: String?) {
        if (this.accessToken == accessToken) return
        this.accessToken = accessToken
        if (accessToken == null) {
            libraryJob?.cancel()
            _uiState.value = _uiState.value.copy(items = emptyList(), showLoading = false, errorMessage = null)
        } else {
            load()
        }
    }

    fun retry() = load()

    fun remove(bookIds: Set<String>) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        mutate { token -> repository.bulkDelete(serverBookIds, token) }
    }

    fun changeStatus(bookIds: Set<String>, status: ReadingStatus) {
        val serverBookIds = _uiState.value.items.filter { it.id in bookIds }.map(ArchiveBookUiModel::bookId)
        if (serverBookIds.isEmpty()) return
        mutate { token -> repository.bulkChangeStatus(serverBookIds, status.apiValue, token) }
    }

    private fun load() {
        val token = accessToken ?: return
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch { loadIntoState(token) }
    }

    private fun mutate(action: suspend (String) -> Unit) {
        val token = accessToken ?: return
        libraryJob?.cancel()
        libraryJob = viewModelScope.launch {
            mutationMutex.withLock {
                withDelayedLoading(::setLoading) {
                    action(token)
                    repository.getAll(token)
                }.onSuccess { books ->
                    if (accessToken == token) {
                        _uiState.value = _uiState.value.copy(
                            items = books.map { it.toArchiveBookUiModel() },
                            errorMessage = null,
                        )
                    }
                }.onFailure(::handleFailure)
            }
        }
    }

    private suspend fun loadIntoState(token: String) {
        withDelayedLoading(::setLoading) { repository.getAll(token) }
            .onSuccess { books ->
                if (accessToken == token) {
                    _uiState.value = _uiState.value.copy(
                        items = books.map { it.toArchiveBookUiModel() },
                        errorMessage = null,
                    )
                }
            }
            .onFailure(::handleFailure)
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(errorMessage = "서재를 불러오지 못했어요")
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(showLoading = loading)
    }
}
