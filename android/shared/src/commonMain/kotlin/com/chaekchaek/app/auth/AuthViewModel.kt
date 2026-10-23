package com.chaekchaek.app.auth

import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.presentation.common.ErrorOperation
import androidx.lifecycle.ViewModel
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthTokens
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.chaekchaek.app.data.remote.MemberRemoteRepository
import com.chaekchaek.app.data.remote.RemoteMemberProfile
import com.chaekchaek.app.domain.reader.Nickname
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class AuthUiState(
  val signingIn: Boolean = false,
  val checkingProfile: Boolean = false,
  val errorMessage: String? = null,
  val requiresNickname: Boolean = false,
  val savingNickname: Boolean = false,
)

class AuthViewModel private constructor(
  private val callbacks: AuthPlatformCallbacks,
  private val getProfile: suspend (String) -> RemoteMemberProfile,
  private val updateNickname: suspend (String, String) -> RemoteMemberProfile,
  private val loginWithGoogle: suspend (String, String?) -> MobileAuthTokens,
  private val loginWithApple: suspend (AppleSignInCredential, String?) -> MobileAuthTokens,
  reissue: suspend (String) -> MobileAuthTokens,
  logout: suspend (String) -> Unit,
  private val scope: CoroutineScope,
  private val ownsScope: Boolean,
  currentTimeMillis: () -> Long,
  errorReporter: com.chaekchaek.app.analytics.UserErrorReporter,
) : ViewModel() {
  constructor(
    callbacks: AuthPlatformCallbacks,
    remoteRepository: MobileAuthRemoteRepository = MobileAuthRemoteRepository(),
    memberRepository: MemberRemoteRepository = MemberRemoteRepository(),
    errorReporter: com.chaekchaek.app.analytics.UserErrorReporter = com.chaekchaek.app.analytics.UserErrorReporter.None,
  ) : this(
    callbacks = callbacks,
    getProfile = memberRepository::get,
    updateNickname = memberRepository::updateNickname,
    loginWithGoogle = remoteRepository::loginWithGoogle,
    loginWithApple = { credential, guestToken ->
      remoteRepository.loginWithApple(
        credential.identityToken,
        credential.authorizationCode,
        credential.nonce,
        guestToken,
      )
    },
    reissue = remoteRepository::reissue,
    logout = remoteRepository::logout,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    ownsScope = true,
    currentTimeMillis = { Clock.System.now().toEpochMilliseconds() },
    errorReporter = errorReporter,
  )

  internal constructor(
    callbacks: AuthPlatformCallbacks,
    getProfile: suspend (String) -> RemoteMemberProfile,
    updateNickname: suspend (String, String) -> RemoteMemberProfile = { _, _ -> error("예상하지 않은 닉네임 저장") },
    loginWithGoogle: suspend (String, String?) -> MobileAuthTokens,
    loginWithApple: suspend (AppleSignInCredential, String?) -> MobileAuthTokens = { _, _ ->
      error("예상하지 않은 Apple 로그인")
    },
    reissue: suspend (String) -> MobileAuthTokens,
    logout: suspend (String) -> Unit,
    scope: CoroutineScope,
    currentTimeMillis: () -> Long,
  ) : this(callbacks, getProfile, updateNickname, loginWithGoogle, loginWithApple, reissue, logout, scope, false, currentTimeMillis, com.chaekchaek.app.analytics.UserErrorReporter.None)

  val errors = ErrorRecoveryState(errorReporter)

  private val session = AuthSession(
    readRefreshToken = callbacks.readRefreshToken,
    writeRefreshToken = callbacks.writeRefreshToken,
    clearRefreshToken = callbacks.clearRefreshToken,
    reissue = reissue,
    logout = logout,
    scope = scope,
    currentTimeMillis = currentTimeMillis,
  )
  private val readyTokens = MutableStateFlow<MobileAuthTokens?>(null)
  val tokens: StateFlow<MobileAuthTokens?> = readyTokens.asStateFlow()
  val appleSignInAvailable: Boolean = callbacks.requestAppleCredential != null

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
  val isRestoringSession: StateFlow<Boolean> = combine(session.isRestoring, uiState) { restoring, state ->
    restoring || state.checkingProfile
  }.stateIn(scope, SharingStarted.Eagerly, session.isRestoring.value)
  private var authenticationGeneration = 0
  private var authenticationJob: Job? = null
  private var pendingAction: (suspend (accessToken: String) -> Unit)? = null

  // 보안 세션은 복원하되 공개 닉네임이 확인된 뒤에만 화면과 대기 작업에 전달한다.
  private var profileReady = false
  private var profileJob: Job? = null

  init {
    scope.launch {
      session.tokens.collectLatest { current ->
        if (current == null) {
          profileReady = false
          readyTokens.value = null
          if (_uiState.value.requiresNickname || _uiState.value.checkingProfile) _uiState.value = AuthUiState()
        } else if (profileReady) {
          readyTokens.value = current
        } else {
          checkProfile(current)
        }
      }
    }
  }

  private suspend fun checkProfile(current: MobileAuthTokens) {
    _uiState.value = _uiState.value.copy(checkingProfile = true)
    try {
      val profile = getProfile(current.accessToken)
      if (session.tokens.value != current) return
      if (profile.nickname.isBlank()) {
        _uiState.value = AuthUiState(requiresNickname = true)
      } else completeAuthentication(current)
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      if (session.tokens.value != current) return
      _uiState.value = AuthUiState(errorMessage = LOGIN_ERROR)
      errors.report(error, ErrorOperation.Settings) {
        profileJob = scope.launch { session.tokens.value?.let { checkProfile(it) } }
      }
    }
  }

  fun saveNickname(nickname: String) {
    val value = nickname.trim()
    if (!Nickname.isValid(value)) {
      errors.confirm("공백이 아닌 최대 10자의 닉네임을 입력해 주세요.")
      return
    }
    if (_uiState.value.savingNickname) return
    val current = session.tokens.value ?: return
    _uiState.value = AuthUiState(requiresNickname = true, savingNickname = true)
    profileJob = scope.launch {
      try {
        val profile = updateNickname(value, current.accessToken)
        if (session.tokens.value != current) return@launch
        check(profile.nickname.isNotBlank())
        completeAuthentication(current)
      } catch (error: CancellationException) {
        throw error
      } catch (error: Exception) {
        if (session.tokens.value == current) {
          _uiState.value = AuthUiState(requiresNickname = true)
          if ((error as? io.ktor.client.plugins.ResponseException)?.response?.status?.value == 409) {
            errors.confirm("이미 사용 중인 닉네임이에요. 다른 닉네임을 입력해 주세요.")
          } else errors.report(error, ErrorOperation.Settings) { saveNickname(value) }
        }
      }
    }
  }

  private suspend fun completeAuthentication(current: MobileAuthTokens) {
    profileReady = true
    readyTokens.value = current
    _uiState.value = AuthUiState()
    val resume = pendingAction
    pendingAction = null
    val generation = authenticationGeneration
    if (resume != null) scope.launch {
      if (authenticationGeneration == generation && profileReady) resume(current.accessToken)
    }
  }

  fun requireAuthentication(force: Boolean = false, action: suspend (accessToken: String) -> Unit) {
    requestAuthentication(action, force, callbacks.requestGoogleIdToken) { idToken ->
      loginWithGoogle(idToken, callbacks.readGuest()?.token)
    }
  }

  fun requireAppleAuthentication(force: Boolean = false, action: suspend (accessToken: String) -> Unit) {
    val requestAppleCredential = callbacks.requestAppleCredential ?: return
    requestAuthentication(action, force, requestAppleCredential) { credential ->
      loginWithApple(credential, callbacks.readGuest()?.token)
    }
  }

  private fun <Credential> requestAuthentication(
    action: suspend (accessToken: String) -> Unit,
    force: Boolean,
    requestCredential: ((Credential?, String?) -> Unit) -> Unit,
    login: suspend (Credential) -> MobileAuthTokens,
  ) {
    val accessToken = tokens.value?.accessToken
    if (accessToken != null && !force) {
      scope.launch { action(accessToken) }
      return
    }

    val original = pendingAction.takeIf { force && !profileReady && session.tokens.value != null }
    pendingAction = if (original == null) action else { token -> original(token); action(token) }
    if (_uiState.value.signingIn || (_uiState.value.requiresNickname && !force)) return
    if (!force && session.tokens.value != null) {
      errors.dismiss()
      profileJob?.cancel()
      profileJob = scope.launch { session.tokens.value?.let { checkProfile(it) } }
      return
    }
    val generation = ++authenticationGeneration
    errors.dismiss()
    var credentialConsumed = false
    _uiState.value = AuthUiState(signingIn = true)
    requestCredential { credential, platformError ->
      if (generation != authenticationGeneration || credentialConsumed) return@requestCredential
      credentialConsumed = true
      if (credential == null) {
        _uiState.value = AuthUiState(errorMessage = platformError?.let { LOGIN_ERROR })
        if (platformError != null) errors.report(IllegalStateException(), ErrorOperation.SignIn) {
          requestAuthentication(action, force, requestCredential, login)
        }
        return@requestCredential
      }

      authenticationJob = scope.launch {
        try {
          val tokens = login(credential)
          if (generation != authenticationGeneration) return@launch
          profileReady = false
          // 재인증의 프로필 확인 중에도 이미 확인된 세션과 오류 복구 작업을 유지한다.
          // 새 토큰은 프로필 확인 후에만 전달하고 실제 로그아웃에서만 기존 토큰을 지운다.
          session.signIn(tokens)
          callbacks.clearGuest()
        } catch (error: CancellationException) {
          throw error
        } catch (error: Exception) {
          if (generation == authenticationGeneration) {
            _uiState.value = AuthUiState(errorMessage = LOGIN_ERROR)
            errors.report(error, ErrorOperation.SignIn) {
              requestAuthentication(action, force, requestCredential, login)
            }
          }
        }
      }
    }
  }

  fun cancelPendingAuthentication() {
    if (_uiState.value.requiresNickname) return
    errors.dismiss()
    authenticationGeneration += 1
    authenticationJob?.cancel()
    authenticationJob = null
    pendingAction = null
    _uiState.value = AuthUiState()
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  suspend fun refreshAccessTokenForRetry(): String? = session.refreshAccessTokenForRetry()

  fun signOut(onLocalSessionCleared: () -> Unit = {}) {
    profileJob?.cancel()
    profileReady = false
    readyTokens.value = null
    _uiState.value = AuthUiState()
    cancelPendingAuthentication()
    scope.launch { session.signOut(onLocalSessionCleared) }
  }

  fun close() {
    if (ownsScope) scope.cancel()
  }

  override fun onCleared() {
    close()
  }

  private companion object {
    const val LOGIN_ERROR = "로그인에 실패했어요. 다시 시도해 주세요."
  }
}
