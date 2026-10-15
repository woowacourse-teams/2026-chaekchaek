package com.chaekchaek.app.auth

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
  private val mutex = Mutex()
  private val renewalRequestMutex = Mutex()
  private var accessTokenExpiresAtMillis = 0L
  private var renewalJob: Job? = null
  private var renewalRequest: Deferred<Result<MobileAuthTokens?>>? = null

  init {
    startRenewal()
  }

  suspend fun signIn(tokens: MobileAuthTokens) {
    mutex.withLock {
      updateTokens(tokens)
      startRenewal()
    }
  }

  suspend fun signOut(onLocalSessionCleared: () -> Unit = {}) {
    val refreshToken = mutex.withLock {
      renewalJob?.cancel()
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

  private suspend fun renewTokensOnce(): MobileAuthTokens? {
    val request = renewalRequestMutex.withLock {
      renewalRequest ?: scope.async {
        try {
          Result.success(renewTokensLocked())
        } catch (error: CancellationException) {
          throw error
        } catch (error: Throwable) {
          Result.failure(error)
        }
      }.also { renewalRequest = it }
    }
    return try {
      request.await().getOrThrow()
    } finally {
      renewalRequestMutex.withLock {
        if (renewalRequest === request) renewalRequest = null
      }
    }
  }

  private suspend fun renewTokensLocked(): MobileAuthTokens? = mutex.withLock {
    val refreshToken = _tokens.value?.refreshToken ?: readRefreshToken()
    if (refreshToken == null) {
      _isRestoring.value = false
      return@withLock null
    }
    try {
      reissue(refreshToken).also(::updateTokens)
    } catch (error: MobileLoginException) {
      if (error.statusCode != PERMANENT_AUTH_FAILURE_STATUS) throw error
      clear()
      null
    }
  }

  private fun updateTokens(tokens: MobileAuthTokens) {
    _tokens.value = tokens
    _isRestoring.value = false
    accessTokenExpiresAtMillis = currentTimeMillis() + tokens.accessTokenExpiresIn * MILLIS_PER_SECOND
    writeRefreshToken(tokens.refreshToken)
  }

  private fun clear() {
    _tokens.value = null
    _isRestoring.value = false
    accessTokenExpiresAtMillis = 0L
    clearRefreshToken()
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
