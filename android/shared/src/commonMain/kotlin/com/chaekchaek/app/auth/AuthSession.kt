package com.chaekchaek.app.auth

import com.chaekchaek.app.data.remote.AuthenticatedRequest
import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class AuthTokenDebugInfo(
  val hasAccessToken: Boolean,
  val accessTokenExpiresInSeconds: Long?,
  val hasRefreshToken: Boolean,
  val refreshTokenExpiresInSeconds: Long?,
  /** 앱 실행 후 서버로 보낸 재발급 요청 수. 동시 401의 단일 재발급을 기기에서 확인하는 개발자 도구용 값이다. */
  val reissueRequestsSent: Int,
)

internal data class SessionCredentials(val tokens: MobileAuthTokens, val generation: Long, val profileRequestKey: String)
internal data class MemberAuthorization(val requestKey: String, val generation: Long)

@OptIn(ExperimentalUuidApi::class)
internal class AuthSession(
  private val readRefreshToken: () -> String?,
  private val writeRefreshToken: (String) -> Unit,
  private val clearRefreshToken: () -> Unit,
  private val reissue: suspend (String) -> MobileAuthTokens,
  private val logout: suspend (String) -> Unit,
  private val scope: CoroutineScope,
  private val currentTimeMillis: () -> Long,
) {
  private val _credentials = MutableStateFlow<SessionCredentials?>(null)
  val credentials = _credentials.asStateFlow()
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
  private var profileRequestKey = Uuid.random().toString()
  private var renewal = Renewal()
  private var memberRequestKey: String? = null
  private var debugOperation: CompletableDeferred<Unit>? = null
  private var reissueRequestsSent = 0

  private class Renewal {
    var request: Deferred<Result<MobileAuthTokens?>>? = null
  }

  init {
    startRenewal()
  }

  suspend fun signIn(tokens: MobileAuthTokens) {
    mutex.withLock {
      sessionGeneration++
      profileRequestKey = Uuid.random().toString()
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
      val refreshToken = credentials.value?.tokens?.refreshToken ?: readRefreshToken()
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

  suspend fun bindMemberRequestKey(key: String, expectedGeneration: Long? = null): SessionCredentials? = mutex.withLock {
    val current = credentials.value ?: return@withLock null
    if (expectedGeneration != null && current.generation != expectedGeneration) return@withLock null
    memberRequestKey = key
    _requiresLogin.value = false
    current
  }

  suspend fun prepareRequest(key: String): AuthenticatedRequest = mutex.withLock {
    val current = credentials.value
    val member = memberRequestKey?.takeIf { it == key }?.let { MemberAuthorization(it, sessionGeneration) }
    if (member == null && key != current?.profileRequestKey) throw CancellationException("인증 세션이 변경되었습니다")
    val accessToken = current?.tokens?.accessToken
      ?: throw MobileLoginException("UNAUTHORIZED", 401, member?.requestKey, member?.generation)
    val generation = sessionGeneration
    val attempt = renewal
    AuthenticatedRequest(accessToken, member) {
      renewTokensOnce(generation, attempt, accessToken)?.accessToken
    }
  }

  fun tokenDebugInfo(): AuthTokenDebugInfo {
    val current = credentials.value?.tokens
    val hasRefresh = current?.refreshToken != null || readRefreshToken() != null
    if (current == null) {
      return AuthTokenDebugInfo(
        hasAccessToken = false,
        accessTokenExpiresInSeconds = null,
        hasRefreshToken = hasRefresh,
        refreshTokenExpiresInSeconds = null,
        reissueRequestsSent = reissueRequestsSent,
      )
    }
    val remaining = ((accessTokenExpiresAtMillis - currentTimeMillis()) / MILLIS_PER_SECOND).coerceAtLeast(0L)
    return AuthTokenDebugInfo(
      hasAccessToken = true,
      accessTokenExpiresInSeconds = remaining,
      hasRefreshToken = hasRefresh,
      refreshTokenExpiresInSeconds = ((refreshTokenExpiresAtMillis - currentTimeMillis()) / MILLIS_PER_SECOND)
        .coerceAtLeast(0L),
      reissueRequestsSent = reissueRequestsSent,
    )
  }

  suspend fun expireAccessTokenForTesting() = withDebugOperation { generation ->
    mutex.withLock {
      check(generation == sessionGeneration) { "인증 세션이 변경되었습니다" }
      val current = checkNotNull(credentials.value) { "만료시킬 Access 토큰이 없습니다" }
      accessTokenExpiresAtMillis = 0L
      _credentials.value = current.copy(
        tokens = current.tokens.copy(accessToken = "expired-token-for-testing", accessTokenExpiresIn = 0),
      )
    }
  }

  suspend fun revokeRefreshTokenForTesting() = withDebugOperation { generation ->
    val refreshToken = mutex.withLock {
      check(generation == sessionGeneration) { "인증 세션이 변경되었습니다" }
      checkNotNull(credentials.value?.tokens?.refreshToken ?: readRefreshToken()) { "폐기할 Refresh 토큰이 없습니다" }
    }
    logout(refreshToken)
    mutex.withLock {
      if (generation == sessionGeneration) refreshTokenExpiresAtMillis = 0L
    }
  }

  private suspend fun withDebugOperation(action: suspend (Long) -> Unit) = debugOperationMutex.withLock {
    val operation = CompletableDeferred<Unit>()
    val (generation, pendingRenewal) = mutex.withLock {
      debugOperation = operation
      renewalJob?.cancel()
      sessionGeneration to renewal.request
    }
    var completed = false
    try {
      pendingRenewal?.join()
      action(generation)
      completed = true
    } finally {
      withContext(NonCancellable) {
        mutex.withLock {
          debugOperation = null
          if (!completed && generation == sessionGeneration && credentials.value != null) startRenewal()
        }
        operation.complete(Unit)
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
    while (true) {
      val operation = mutex.withLock { debugOperation }
      operation?.await()
      val (generation, request) = findRenewal(expectedGeneration, requestAttempt, rejectedAccessToken)
        ?: continue
      val result = request?.await()
      return mutex.withLock {
        if (generation != sessionGeneration) {
          if (expectedGeneration != null) throw CancellationException("인증 세션이 변경되었습니다")
          null
        } else {
          result?.getOrThrow()
          credentials.value?.tokens
        }
      }
    }
  }

  private suspend fun findRenewal(
    expectedGeneration: Long?,
    requestAttempt: Renewal?,
    rejectedAccessToken: String?,
  ): Pair<Long, Deferred<Result<MobileAuthTokens?>>?>? =
    mutex.withLock {
      val generation = sessionGeneration
      if (expectedGeneration != null && generation != expectedGeneration) {
        throw CancellationException("인증 세션이 변경되었습니다")
      }
      val current = credentials.value?.tokens
      if (rejectedAccessToken != null && current?.accessToken != rejectedAccessToken) return@withLock generation to null
      val attempt = requestAttempt ?: renewal
      val request = attempt.request ?: run {
        if (debugOperation != null) return@withLock null
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

  private suspend fun renewCurrentSessionTokens(generation: Long): MobileAuthTokens? {
    val refreshToken = mutex.withLock {
      if (generation != sessionGeneration) return null
      val refreshToken = credentials.value?.tokens?.refreshToken ?: readRefreshToken()
      if (refreshToken == null) {
        _isRestoring.value = false
        return null
      }
      reissueRequestsSent++
      refreshToken
    }
    return try {
      val renewed = reissue(refreshToken)
      mutex.withLock {
        if (sessionGeneration != generation) return@withLock null
        updateTokens(renewed)
        if (renewalJob?.isActive != true && debugOperation == null) startRenewal()
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
    _credentials.value = SessionCredentials(tokens, sessionGeneration, profileRequestKey)
    _isRestoring.value = false
  }

  private fun clear() {
    accessTokenExpiresAtMillis = 0L
    refreshTokenExpiresAtMillis = 0L
    clearRefreshToken()
    _credentials.value = null
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
