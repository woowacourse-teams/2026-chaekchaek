package com.chamsae.chaekchaek.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthSession(
  private val refreshTokenStore: RefreshTokenStore,
  private val remoteRepository: MobileAuthRemoteRepository,
  private val currentTimeMillis: () -> Long = System::currentTimeMillis,
  private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
  private val _tokens = MutableStateFlow<MobileAuthTokens?>(null)
  val tokens: StateFlow<MobileAuthTokens?> = _tokens.asStateFlow()
  private val mutex = Mutex()
  private var accessTokenExpiresAtMillis = 0L
  private var renewalJob: Job? = null

  init {
    startRenewal()
  }

  fun signIn(tokens: MobileAuthTokens) {
    updateTokens(tokens)
    startRenewal()
  }

  suspend fun signOut() {
    renewalJob?.cancel()
    mutex.withLock {
      val refreshToken = _tokens.value?.refreshToken ?: refreshTokenStore.read()
      try {
        if (refreshToken != null) remoteRepository.logout(refreshToken)
      } finally {
        clear()
      }
    }
  }

  private fun startRenewal() {
    renewalJob?.cancel()
    renewalJob = scope.launch {
      while (isActive) {
        val waitMillis = (accessTokenExpiresAtMillis - currentTimeMillis() - EXPIRY_MARGIN_MILLIS).coerceAtLeast(0L)
        delay(waitMillis)
        try {
          mutex.withLock {
            val refreshToken = _tokens.value?.refreshToken ?: refreshTokenStore.read() ?: return@launch
            updateTokens(remoteRepository.reissue(refreshToken))
          }
        } catch (error: MobileLoginException) {
          if (error.statusCode in 400..499) {
            clear()
            return@launch
          }
          delay(RETRY_DELAY_MILLIS)
        } catch (_: Exception) {
          delay(RETRY_DELAY_MILLIS)
        }
      }
    }
  }

  private fun updateTokens(tokens: MobileAuthTokens) {
    _tokens.value = tokens
    accessTokenExpiresAtMillis = currentTimeMillis() + tokens.accessTokenExpiresIn * MILLIS_PER_SECOND
    refreshTokenStore.write(tokens.refreshToken)
  }

  private fun clear() {
    _tokens.value = null
    accessTokenExpiresAtMillis = 0L
    refreshTokenStore.clear()
  }

  internal companion object {
    private const val MILLIS_PER_SECOND = 1_000L
    private const val EXPIRY_MARGIN_MILLIS = 30_000L
    private const val RETRY_DELAY_MILLIS = 30_000L

    fun renewalDelayMillis(expiresAtMillis: Long, currentTimeMillis: Long): Long =
      (expiresAtMillis - currentTimeMillis - EXPIRY_MARGIN_MILLIS).coerceAtLeast(0L)
  }
}
