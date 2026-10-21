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
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class AuthUiState(
  val signingIn: Boolean = false,
  val errorMessage: String? = null,
)

class AuthViewModel private constructor(
  private val callbacks: AuthPlatformCallbacks,
  private val loginWithGoogle: suspend (String, String?) -> MobileAuthTokens,
  private val loginWithApple: suspend (AppleSignInCredential, String?) -> MobileAuthTokens,
  reissue: suspend (String) -> MobileAuthTokens,
  logout: suspend (String) -> Unit,
  private val scope: CoroutineScope,
  private val ownsScope: Boolean,
  currentTimeMillis: () -> Long,
) : ViewModel() {
  constructor(
    callbacks: AuthPlatformCallbacks,
    remoteRepository: MobileAuthRemoteRepository = MobileAuthRemoteRepository(),
  ) : this(
    callbacks = callbacks,
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
  )

  internal constructor(
    callbacks: AuthPlatformCallbacks,
    loginWithGoogle: suspend (String, String?) -> MobileAuthTokens,
    loginWithApple: suspend (AppleSignInCredential, String?) -> MobileAuthTokens = { _, _ ->
      error("예상하지 않은 Apple 로그인")
    },
    reissue: suspend (String) -> MobileAuthTokens,
    logout: suspend (String) -> Unit,
    scope: CoroutineScope,
    currentTimeMillis: () -> Long,
  ) : this(callbacks, loginWithGoogle, loginWithApple, reissue, logout, scope, false, currentTimeMillis)

  val errors = ErrorRecoveryState()

  private val session = AuthSession(
    readRefreshToken = callbacks.readRefreshToken,
    writeRefreshToken = callbacks.writeRefreshToken,
    clearRefreshToken = callbacks.clearRefreshToken,
    reissue = reissue,
    logout = logout,
    scope = scope,
    currentTimeMillis = currentTimeMillis,
  )
  val tokens: StateFlow<MobileAuthTokens?> = session.tokens
  val isRestoringSession: StateFlow<Boolean> = session.isRestoring
  val appleSignInAvailable: Boolean = callbacks.requestAppleCredential != null

  private val _uiState = MutableStateFlow(AuthUiState())
  val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
  private var authenticationGeneration = 0
  private var authenticationJob: Job? = null
  private var pendingAction: (suspend (accessToken: String) -> Unit)? = null

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

    pendingAction = action
    if (_uiState.value.signingIn) return
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
          session.signIn(tokens)
          callbacks.clearGuest()
          val actionToResume = pendingAction
          this@AuthViewModel.pendingAction = null
          actionToResume?.invoke(tokens.accessToken)
          _uiState.value = AuthUiState()
        } catch (error: CancellationException) {
          throw error
        } catch (_: Exception) {
          if (generation == authenticationGeneration) {
            _uiState.value = AuthUiState(errorMessage = LOGIN_ERROR)
            errors.report(IllegalStateException(), ErrorOperation.SignIn) {
              requestAuthentication(action, force, requestCredential, login)
            }
          }
        }
      }
    }
  }

  fun cancelPendingAuthentication() {
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
