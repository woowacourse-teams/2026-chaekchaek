package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.remote.*
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BookshelfUiState(
    val shelves: List<Bookshelf> = emptyList(),
    val memberId: Long? = null,
    val detail: BookshelfDetail? = null,
    val selectedId: Long? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val detailRefreshing: Boolean = false,
    val detailStale: Boolean = false,
    val collectionStale: Boolean = true,
    val mutating: Boolean = false,
    val loaded: Boolean = false,
)

/** 책장 배치를 변경하며 독서 기록·별점의 저장 책임은 기존 저장소에 둔다. */
class BookshelfViewModel(private val repository: BookshelfRepository) : ViewModel() {
    val errors = ErrorRecoveryState()
    private val state = MutableStateFlow(BookshelfUiState())
    val uiState = state.asStateFlow()
    private var token: String? = null
    private var authenticatedMemberId: Long? = null
    private var generation = 0
    private var readJob: Job? = null
    private var detailJob: Job? = null
    private var mutationJob: Job? = null
    private var detailGeneration = 0

    fun authenticate(value: String?, memberId: Long? = null) {
        val memberChanged = authenticatedMemberId != null && memberId != null && authenticatedMemberId != memberId
        if (value == null || memberId != null) authenticatedMemberId = memberId
        if (token == value && !memberChanged) return
        if (!memberChanged && token != null && value != null) { token = value; return }
        generation++
        errors.dismiss()
        readJob?.cancel(); detailJob?.cancel(); mutationJob?.cancel()
        token = value
        state.value = BookshelfUiState(memberId = memberId)
        if (value != null) refresh()
    }

    fun refresh() {
        if (token == null || state.value.mutating) return
        val session = generation
        readJob?.cancel()
        state.value = state.value.copy(refreshing = true, collectionStale = true, detailStale = state.value.detail != null || state.value.detailStale)
        readJob = viewModelScope.launch {
            try {
                withDelayedLoading({ if (session == generation) state.value = state.value.copy(loading = it) }) {
                    repository.mine(token ?: return@withDelayedLoading null)
                }.getOrThrow()?.let { result ->
                    if (session != generation) return@launch
                    state.value = state.value.copy(shelves = result.libraries, memberId = result.memberId, loaded = true, collectionStale = false)
                    state.value.selectedId?.let(::open)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { if (session == generation) report(e) { refresh() } }
            finally { if (session == generation) state.value = state.value.copy(refreshing = false, loading = false) }
        }
    }

    fun open(id: Long) {
        if (token == null) return
        detailJob?.cancel()
        val session = generation
        val request = ++detailGeneration
        state.value = state.value.copy(selectedId = id, detail = state.value.detail?.takeIf { it.libraryId == id }, detailRefreshing = true, detailStale = true)
        detailJob = viewModelScope.launch {
            try {
                val detail = withDelayedLoading({ if (request == detailGeneration) state.value = state.value.copy(loading = it) }) {
                    repository.detail(id)
                }.getOrThrow()
                if (session == generation && request == detailGeneration) state.value = state.value.copy(detail = detail, detailStale = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { if (session == generation && request == detailGeneration) report(e) { open(id) } }
            finally { if (session == generation && request == detailGeneration) state.value = state.value.copy(loading = false, detailRefreshing = false) }
        }
    }

    fun close() {
        detailGeneration++
        detailJob?.cancel()
        state.value = state.value.copy(selectedId = null, detail = null, loading = false, detailRefreshing = false)
    }

    fun create(name: String, done: () -> Unit) {
        if (!validateName(name)) return
        mutate(done) { repository.create(name, it) }
    }

    fun rename(id: Long, name: String, done: () -> Unit) {
        if (!validateName(name, id)) return
        mutate(done) { repository.rename(id, name, it) }
    }

    fun delete(id: Long, done: () -> Unit) {
        if (state.value.shelves.firstOrNull { it.libraryId == id }?.isDefault == true) {
            errors.confirm("기본 책장은 삭제할 수 없어요"); return
        }
        mutate({ if (state.value.selectedId == id) close(); done() }) { repository.delete(id, it) }
    }

    fun remove(libraryId: Long, bookId: Long) = mutate { repository.remove(libraryId, bookId, it) }

    fun orderShelves(ids: List<Long>) {
        if (state.value.refreshing || state.value.collectionStale || !state.value.loaded) return
        if (!sameIds(ids, state.value.shelves.map { it.libraryId })) return
        mutate { repository.orderShelves(ids, it) }
    }

    fun orderBooks(libraryId: Long, ids: List<Long>) {
        if (state.value.refreshing || state.value.collectionStale || state.value.detailRefreshing ||
            (state.value.selectedId == libraryId && state.value.detailStale)) return
        val existing = state.value.detail?.takeIf { it.libraryId == libraryId }?.items?.map { it.bookId }
            ?: state.value.shelves.firstOrNull { it.libraryId == libraryId }?.items?.map { it.bookId } ?: return
        if (!sameIds(ids, existing)) return
        mutate { repository.orderBooks(libraryId, ids, it) }
    }

    private fun validateName(name: String, id: Long? = null): Boolean {
        val error = bookshelfNameError(name, state.value.shelves.filter { it.libraryId != id }.map { it.name })
        if (error != null) errors.confirm(error)
        return error == null
    }

    private fun mutate(done: () -> Unit = {}, action: suspend (String) -> Unit) {
        if (token == null || state.value.mutating) return
        val session = generation
        readJob?.cancel()
        detailGeneration++
        detailJob?.cancel()
        state.value = state.value.copy(loading = false, mutating = true, refreshing = false, detailRefreshing = false)
        mutationJob = viewModelScope.launch {
            try {
                withDelayedLoading({ if (session == generation) state.value = state.value.copy(loading = it) }) {
                    action(token ?: return@withDelayedLoading)
                }.getOrThrow()
                if (session == generation) {
                    done()
                    state.value = state.value.copy(mutating = false, detailStale = true, collectionStale = true)
                    refresh()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { if (session == generation) report(e) { mutate(done, action) } }
            finally { if (session == generation) state.value = state.value.copy(mutating = false, loading = false) }
        }
    }

    private fun report(error: Throwable, retry: () -> Unit) {
        errors.report(error, ErrorOperation.Request) { freshToken ->
            if (freshToken != null) token = freshToken
            retry()
        }
    }
}

internal val BookshelfNameInputPolicy = com.chaekchaek.app.ui.input.TextInputPolicy(10, utf16 = true)
internal fun bookshelfNameError(name: String, otherNames: List<String>): String? = when {
    name.isBlank() || name.length > 10 -> "책장 이름은 공백만 제외하고 1-10자로 입력해 주세요"
    otherNames.any { it.equals(name, ignoreCase = true) } -> "같은 이름의 책장이 있어요"
    else -> null
}
internal fun sameIds(proposed: List<Long>, existing: List<Long>): Boolean =
    proposed.size == existing.size && proposed.distinct().size == proposed.size && proposed.toSet() == existing.toSet()
