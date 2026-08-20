package com.chamsae.chaekchaek.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthSession internal constructor(
  private val readRefreshToken: () -> String?,
  private val writeRefreshToken: (String) -> Unit,
  private val clearRefreshToken: () -> Unit,
  private val reissue: suspend (String) -> MobileAuthTokens,
  private val logout: suspend (String) -> Unit,
  private val scope: CoroutineScope,
  private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
  private val _tokens = MutableStateFlow<MobileAuthTokens?>(null)
  val tokens: StateFlow<MobileAuthTokens?> = _tokens.asStateFlow()
  private val mutex = Mutex()
  private var accessTokenExpiresAtMillis = 0L
  private var renewalJob: Job? = null

  init {
    startRenewal()
  }

  constructor(
    refreshTokenStore: RefreshTokenStore,
    remoteRepository: MobileAuthRemoteRepository,
    scope: CoroutineScope,
    currentTimeMillis: () -> Long = System::currentTimeMillis,
  ) : this(
    readRefreshToken = refreshTokenStore::read,
    writeRefreshToken = refreshTokenStore::write,
    clearRefreshToken = refreshTokenStore::clear,
    reissue = remoteRepository::reissue,
    logout = remoteRepository::logout,
    scope = scope,
    currentTimeMillis = currentTimeMillis,
  )

  suspend fun signIn(tokens: MobileAuthTokens) {
    mutex.withLock {
      updateTokens(tokens)
      startRenewal()
    }
  }

  suspend fun signOut() {
    mutex.withLock {
      renewalJob?.cancel()
      val refreshToken = _tokens.value?.refreshToken ?: readRefreshToken()
      try {
        if (refreshToken != null) logout(refreshToken)
      } finally {
        clear()
      }
    }
  }

  private fun startRenewal() {
    renewalJob?.cancel()
    renewalJob = scope.launch {
      while (isActive) {
        delay(renewalDelayMillis(accessTokenExpiresAtMillis, currentTimeMillis()))
        try {
          if (!renewTokens()) return@launch
        } catch (_: MobileLoginException) {
          delay(RETRY_DELAY_MILLIS)
        } catch (_: Exception) {
          delay(RETRY_DELAY_MILLIS)
        }
      }
    }
  }

  private suspend fun renewTokens(): Boolean = mutex.withLock {
    val refreshToken = _tokens.value?.refreshToken ?: readRefreshToken() ?: return@withLock false
    try {
      updateTokens(reissue(refreshToken))
      true
    } catch (error: MobileLoginException) {
      if (error.statusCode != PERMANENT_AUTH_FAILURE_STATUS) throw error
      clear()
      false
    }
  }

  private fun updateTokens(tokens: MobileAuthTokens) {
    _tokens.value = tokens
    accessTokenExpiresAtMillis = currentTimeMillis() + tokens.accessTokenExpiresIn * MILLIS_PER_SECOND
    writeRefreshToken(tokens.refreshToken)
  }

  private fun clear() {
    _tokens.value = null
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
