package com.chaekchaek.app.ui.publiclibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicReadState<T>(val content: T? = null, val loading: Boolean = false, val showLoading: Boolean = false)

/** 응답은 entry의 ViewModel에 두고, 필터와 스크롤은 화면의 saveable 상태로 둔다. */
internal class PublicLibraryViewModel<T>(private val read: suspend () -> T) : ViewModel() {
    private val state = MutableStateFlow(PublicReadState<T>())
    val uiState = state.asStateFlow()
    val errors = ErrorRecoveryState()
    init { retry() }
    fun retry() {
        if (state.value.loading) return
        state.value = state.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val content = withDelayedApiLoading(
                    onLoadingChanged = { state.value = state.value.copy(showLoading = it) },
                    request = read,
                )
                state.value = state.value.copy(content = content)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errors.report(error, ErrorOperation.LibraryLoad, retry = { retry() })
            } finally {
                state.value = state.value.copy(loading = false, showLoading = false)
            }
        }
    }
}
