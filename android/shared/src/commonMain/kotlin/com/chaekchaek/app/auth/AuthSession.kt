package com.chaekchaek.app.auth

import com.chaekchaek.app.data.remote.AuthenticatedRequest
import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AuthTokenDebugInfo(
  val hasAccessToken: Boolean,
  val accessTokenExpiresInSeconds: Long?,
  val hasRefreshToken: Boolean,
  val refreshTokenExpiresInSeconds: Long?,
)

internal class AuthSession(
  private val readRefreshToken: () -> String?,
  private val writeRefreshToken: (String) -> Unit,
  private val clearRefreshToken: () -> Unit,
  private val reissue: suspend (String) -> MobileAuthTokens,
  private val logout: suspend (String) -> Unit,
  private val scope: CoroutineScope,
  private val currentTimeMillis: () -> Long,
) {
  private val _tokens = MutableStateFlow<MobileAuthTokens?>(null)
  val tokens: StateFlow<MobileAuthTokens?> = _tokens.asStateFlow()
  private val _isRestoring = MutableStateFlow(readRefreshToken() != null)
  val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()
  private val _requiresLogin = MutableStateFlow(false)
  val requiresLogin = _requiresLogin.asStateFlow()
  private val mutex = Mutex()
  private val debugOperationMutex = Mutex()
  private var accessTokenExpiresAtMillis = 0L
  private var refreshTokenExpiresAtMillis = 0L
  private var renewalJob: Job? = null
  private var sessionGeneration = 0L
  private var renewal = Renewal()
  private var memberRequestKey: String? = null
  private var debugOperationInProgress = false

  private class Renewal {
    var request: Deferred<Result<MobileAuthTokens?>>? = null
  }

  init {
    startRenewal()
  }

  suspend fun signIn(tokens: MobileAuthTokens) {
    mutex.withLock {
      sessionGeneration++
      memberRequestKey = null
      _requiresLogin.value = false
      renewal = Renewal()
      updateTokens(tokens)
      startRenewal()
    }
  }

  suspend fun signOut(onLocalSessionCleared: () -> Unit = {}) {
    val refreshToken = mutex.withLock {
      renewalJob?.cancel()
      renewal.request?.cancel()
      renewal = Renewal()
      sessionGeneration++
      memberRequestKey = null
      _requiresLogin.value = false
      val refreshToken = _tokens.value?.refreshToken ?: readRefreshToken()
      clear()
      refreshToken
    }
    onLocalSessionCleared()
    try {
      if (refreshToken != null) logout(refreshToken)
    } catch (error: CancellationException) {
      throw error
    } catch (_: Exception) {
      // 로컬 로그아웃은 완료됐으므로 원격 토큰 폐기 실패로 세션을 복원하지 않는다.
    }
  }

  suspend fun refreshAccessTokenForRetry(): String? = renewTokensOnce()?.accessToken

  suspend fun bindMemberRequestKey(key: String, expectedAccessToken: String? = null): Boolean = mutex.withLock {
    if (expectedAccessToken != null && _tokens.value?.accessToken != expectedAccessToken) return@withLock false
    memberRequestKey = key
    _requiresLogin.value = false
    true
  }

  suspend fun prepareRequest(key: String): AuthenticatedRequest = mutex.withLock {
    val accessToken = _tokens.value?.accessToken
    if (key != memberRequestKey && key != accessToken) throw CancellationException("인증 세션이 변경되었습니다")
    if (accessToken == null) throw MobileLoginException("UNAUTHORIZED", 401)
    val generation = sessionGeneration
    val attempt = renewal
    AuthenticatedRequest(accessToken) {
      renewTokensOnce(generation, attempt, accessToken)?.accessToken
    }
  }

  fun tokenDebugInfo(): AuthTokenDebugInfo {
    val current = _tokens.value
    val hasRefresh = current?.refreshToken != null || readRefreshToken() != null
    if (current == null) {
      return AuthTokenDebugInfo(
        hasAccessToken = false,
        accessTokenExpiresInSeconds = null,
        hasRefreshToken = hasRefresh,
        refreshTokenExpiresInSeconds = null,
      )
    }
    val remaining = ((accessTokenExpiresAtMillis - currentTimeMillis()) / MILLIS_PER_SECOND).coerceAtLeast(0L)
    return AuthTokenDebugInfo(
      hasAccessToken = true,
      accessTokenExpiresInSeconds = remaining,
      hasRefreshToken = hasRefresh,
      refreshTokenExpiresInSeconds = ((refreshTokenExpiresAtMillis - currentTimeMillis()) / MILLIS_PER_SECOND)
        .coerceAtLeast(0L),
    )
  }

  suspend fun expireAccessTokenForTesting() = withDebugOperation { generation ->
    mutex.withLock {
      check(generation == sessionGeneration) { "인증 세션이 변경되었습니다" }
      val current = checkNotNull(_tokens.value) { "만료시킬 Access 토큰이 없습니다" }
      accessTokenExpiresAtMillis = 0L
      _tokens.value = current.copy(
        accessToken = "expired-token-for-testing",
        accessTokenExpiresIn = 0,
      )
    }
  }

  suspend fun revokeRefreshTokenForTesting() = withDebugOperation { generation ->
    val refreshToken = mutex.withLock {
      check(generation == sessionGeneration) { "인증 세션이 변경되었습니다" }
      checkNotNull(_tokens.value?.refreshToken ?: readRefreshToken()) { "폐기할 Refresh 토큰이 없습니다" }
    }
    logout(refreshToken)
    mutex.withLock {
      if (generation == sessionGeneration) refreshTokenExpiresAtMillis = 0L
    }
  }

  private suspend fun withDebugOperation(action: suspend (Long) -> Unit) = debugOperationMutex.withLock {
    val (generation, pendingRenewal) = mutex.withLock {
      debugOperationInProgress = true
      renewalJob?.cancel()
      sessionGeneration to renewal.request
    }
    var completed = false
    try {
      pendingRenewal?.join()
      action(generation)
      completed = true
    } finally {
      mutex.withLock {
        debugOperationInProgress = false
        if (!completed && generation == sessionGeneration && _tokens.value != null) startRenewal()
      }
    }
  }

  private fun startRenewal() {
    renewalJob?.cancel()
    renewalJob = scope.launch {
      while (isActive) {
        delay(renewalDelayMillis(accessTokenExpiresAtMillis, currentTimeMillis()))
        try {
          if (renewTokensOnce() == null) return@launch
        } catch (_: MobileLoginException) {
          delay(RETRY_DELAY_MILLIS)
        } catch (_: Exception) {
          delay(RETRY_DELAY_MILLIS)
        }
      }
    }
  }

  private suspend fun renewTokensOnce(
    expectedGeneration: Long? = null,
    requestAttempt: Renewal? = null,
    rejectedAccessToken: String? = null,
  ): MobileAuthTokens? {
    val (generation, request) = mutex.withLock {
      val generation = sessionGeneration
      if (expectedGeneration != null && generation != expectedGeneration) {
        throw CancellationException("인증 세션이 변경되었습니다")
      }
      val current = _tokens.value
      if (rejectedAccessToken != null && current?.accessToken != rejectedAccessToken) return current
      val attempt = requestAttempt ?: renewal
      val request = attempt.request ?: run {
        if (debugOperationInProgress) return null
        scope.async {
          try {
            Result.success(renewCurrentSessionTokens(generation))
          } catch (error: CancellationException) {
            throw error
          } catch (error: Throwable) {
            Result.failure(error)
          } finally {
            mutex.withLock {
              if (renewal === attempt) renewal = Renewal()
            }
          }
        }.also { attempt.request = it }
      }
      generation to request
    }
    val result = request.await()
    return mutex.withLock {
      if (generation != sessionGeneration) {
        if (expectedGeneration != null) throw CancellationException("인증 세션이 변경되었습니다")
        null
      }
      else {
        result.getOrThrow()
        _tokens.value
      }
    }
  }

  private suspend fun renewCurrentSessionTokens(generation: Long): MobileAuthTokens? {
    val refreshToken = mutex.withLock {
      if (generation != sessionGeneration) return null
      val refreshToken = _tokens.value?.refreshToken ?: readRefreshToken()
      if (refreshToken == null) {
        _isRestoring.value = false
        return null
      }
      refreshToken
    }
    return try {
      val renewed = reissue(refreshToken)
      mutex.withLock {
        if (sessionGeneration != generation) return@withLock null
        updateTokens(renewed)
        if (renewalJob?.isActive != true && !debugOperationInProgress) startRenewal()
        renewed
      }
    } catch (error: MobileLoginException) {
      mutex.withLock {
        if (sessionGeneration != generation) return@withLock null
        if (error.statusCode != PERMANENT_AUTH_FAILURE_STATUS) throw error
        clear()
        _requiresLogin.value = memberRequestKey != null
        null
      }
    }
  }

  private fun updateTokens(tokens: MobileAuthTokens) {
    val now = currentTimeMillis()
    accessTokenExpiresAtMillis = now + tokens.accessTokenExpiresIn * MILLIS_PER_SECOND
    refreshTokenExpiresAtMillis = now + tokens.refreshTokenExpiresIn * MILLIS_PER_SECOND
    writeRefreshToken(tokens.refreshToken)
    _tokens.value = tokens
    _isRestoring.value = false
  }

  private fun clear() {
    accessTokenExpiresAtMillis = 0L
    refreshTokenExpiresAtMillis = 0L
    clearRefreshToken()
    _tokens.value = null
    _isRestoring.value = false
  }

  internal companion object {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val EXPIRY_MARGIN_MILLIS = 30_000L
    private const val RETRY_DELAY_MILLIS = 30_000L
    private const val PERMANENT_AUTH_FAILURE_STATUS = 401

    fun renewalDelayMillis(expiresAtMillis: Long, currentTimeMillis: Long): Long =
      (expiresAtMillis - currentTimeMillis - EXPIRY_MARGIN_MILLIS).coerceAtLeast(0L)
  }
}
