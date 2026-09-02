package com.chaekchaek.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaekchaek.app.data.remote.MemberRemoteRepository
import com.chaekchaek.app.data.remote.RemoteMemberProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MemberSettingsUiState(
    val signedIn: Boolean = false,
    val anonymousReviews: Boolean = true,
    val nickname: String = "",
    val anonymousNickname: String = "",
    val profileImageUrl: String? = null,
    val showLoading: Boolean = false,
    val errorMessage: String? = null,
    val withdrawing: Boolean = false,
    val withdrawalErrorMessage: String? = null,
)

class MemberSettingsViewModel(
    private val repository: MemberRemoteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MemberSettingsUiState())
    val uiState: StateFlow<MemberSettingsUiState> = _uiState.asStateFlow()

    private var accessToken: String? = null
    private var requestJob: Job? = null
    private var pendingSettings: PendingMemberSettings? = null

    fun authenticate(accessToken: String?) {
        if (this.accessToken == accessToken) return
        this.accessToken = accessToken
        requestJob?.cancel()
        pendingSettings = null
        if (accessToken == null) {
            _uiState.value = MemberSettingsUiState()
        } else {
            _uiState.value = _uiState.value.copy(signedIn = true)
            load(accessToken)
        }
    }

    fun setAnonymousReviews(anonymous: Boolean, nickname: String = "") {
        val settings = PendingMemberSettings(anonymous, nickname.trim())
        pendingSettings = settings
        save(settings)
    }

    fun retry() {
        val token = accessToken ?: return
        pendingSettings?.let(::save) ?: load(token)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun withdraw(onSuccess: () -> Unit) {
        val token = accessToken ?: return
        requestJob?.cancel()
        _uiState.value = _uiState.value.copy(withdrawalErrorMessage = null)
        requestJob = viewModelScope.launch {
            withDelayedLoading(::setWithdrawing) { repository.withdraw(token) }
                .onSuccess {
                    if (accessToken == token) onSuccess()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    if (accessToken == token) {
                        _uiState.value = _uiState.value.copy(withdrawalErrorMessage = "회원 탈퇴에 실패했어요")
                    }
                }
        }
    }

    private fun load(token: String) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            withDelayedLoading(::setLoading) { repository.get(token) }
                .onSuccess { applyProfile(it, token) }
                .onFailure { handleFailure(it, "회원 설정을 불러오지 못했어요") }
        }
    }

    private fun save(settings: PendingMemberSettings) {
        val token = accessToken ?: return
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            withDelayedLoading(::setLoading) {
                if (!settings.anonymous && settings.nickname != _uiState.value.nickname) {
                    applyProfile(repository.updateNickname(settings.nickname, token), token)
                }
                if (settings.anonymous != _uiState.value.anonymousReviews) {
                    applyProfile(repository.updateAnonymity(settings.anonymous, token), token)
                }
            }.onSuccess {
                if (accessToken == token) {
                    pendingSettings = null
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
            }.onFailure { handleFailure(it, "설정을 변경하지 못했어요") }
        }
    }

    private fun applyProfile(profile: RemoteMemberProfile, token: String) {
        if (accessToken != token) return
        _uiState.value = _uiState.value.copy(
            anonymousReviews = profile.displayAnonymous,
            nickname = profile.nickname,
            anonymousNickname = profile.anonymousNickname,
            profileImageUrl = profile.profileImageUrl,
            errorMessage = null,
        )
    }

    private fun handleFailure(error: Throwable, message: String) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    private fun setLoading(loading: Boolean) {
        _uiState.value = _uiState.value.copy(showLoading = loading)
    }

    private fun setWithdrawing(withdrawing: Boolean) {
        _uiState.value = _uiState.value.copy(withdrawing = withdrawing)
    }
}

private data class PendingMemberSettings(
    val anonymous: Boolean,
    val nickname: String,
)
