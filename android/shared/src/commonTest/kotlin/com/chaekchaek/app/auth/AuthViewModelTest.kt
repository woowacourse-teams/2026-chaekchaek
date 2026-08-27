package com.chaekchaek.app.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  @Test
  fun `Access Token은 만료 30초 전에 재발급한다`() {
    assertEquals(1L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 69_999L))
    assertEquals(0L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 70_000L))
  }

  @Test
  fun `저장한 Refresh Token으로 세션을 복원한다`() = runTest {
    var storedRefreshToken: String? = "stored"
    val session = AuthSession(
      readRefreshToken = { storedRefreshToken },
      writeRefreshToken = { storedRefreshToken = it },
      clearRefreshToken = { storedRefreshToken = null },
      reissue = { tokens("rotated") },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )

    runCurrent()

    assertEquals("rotated", session.tokens.value?.refreshToken)
    assertEquals("rotated", storedRefreshToken)
  }

  @Test
  fun `재발급으로 교체된 Refresh Token을 다음 재발급에 사용한다`() = runTest {
    var attempts = 0
    val backend = FakeAuthBackend(storedRefreshToken = "stored") {
      if (attempts++ == 0) tokens("rotated-once") else tokens("rotated-twice")
    }
    val session = authSession(backend)

    runCurrent()
    advanceTimeBy(RENEWAL_INTERVAL_MILLIS)
    runCurrent()

    assertEquals(listOf("stored", "rotated-once"), backend.reissueRequests)
    assertEquals("rotated-twice", session.tokens.value?.refreshToken)
    assertEquals("rotated-twice", backend.storedRefreshToken)
  }

  @Test
  fun `이전 계정 재발급 실패가 새 로그인의 Token을 지우지 않는다`() = runTest {
    val reissueStarted = CompletableDeferred<Unit>()
    val finishReissue = CompletableDeferred<Unit>()
    val backend = FakeAuthBackend(storedRefreshToken = "account-a") {
      reissueStarted.complete(Unit)
      finishReissue.await()
      throw MobileLoginException("INVALID_REFRESH_TOKEN", 401)
    }
    val session = authSession(backend)
    runCurrent()
    reissueStarted.await()

    val signIn = launch { session.signIn(tokens("account-b")) }
    runCurrent()
    finishReissue.complete(Unit)
    runCurrent()
    signIn.join()

    assertEquals(1, backend.clearCount)
    assertEquals("account-b", session.tokens.value?.refreshToken)
    assertEquals("account-b", backend.storedRefreshToken)
  }

  @Test
  fun `429 응답은 저장 Token을 유지하고 재시도한다`() = runTest {
    var attempts = 0
    val backend = FakeAuthBackend(storedRefreshToken = "stored") {
      if (attempts++ == 0) throw MobileLoginException("HTTP_429", 429)
      tokens("rotated")
    }
    val session = authSession(backend)

    runCurrent()
    assertEquals("stored", backend.storedRefreshToken)
    assertEquals(0, backend.clearCount)

    advanceTimeBy(RETRY_DELAY_MILLIS)
    runCurrent()
    assertEquals("rotated", session.tokens.value?.refreshToken)
  }

  @Test
  fun `500 응답은 저장 Token을 유지하고 재시도한다`() = runTest {
    var attempts = 0
    val backend = FakeAuthBackend(storedRefreshToken = "stored") {
      if (attempts++ == 0) throw MobileLoginException("INTERNAL_SERVER_ERROR", 500)
      tokens("rotated")
    }
    val session = authSession(backend)

    runCurrent()
    assertEquals("stored", backend.storedRefreshToken)
    assertEquals(0, backend.clearCount)

    advanceTimeBy(RETRY_DELAY_MILLIS)
    runCurrent()
    assertEquals("rotated", session.tokens.value?.refreshToken)
  }

  @Test
  fun `로그아웃은 현재 Refresh Token을 폐기한다`() = runTest {
    val backend = FakeAuthBackend()
    val session = authSession(backend)
    runCurrent()
    session.signIn(tokens("signed-in"))

    session.signOut()

    assertEquals(listOf("signed-in"), backend.logoutRequests)
    assertNull(session.tokens.value)
    assertNull(backend.storedRefreshToken)
  }

  @Test
  fun `로그인 뒤 보류 작업을 새 Access Token으로 재개한다`() = runTest {
    var googleResult: ((String?, String?) -> Unit)? = null
    var storedRefreshToken: String? = null
    var resumedWith: String? = null
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { googleResult = it },
        readRefreshToken = { storedRefreshToken },
        writeRefreshToken = { storedRefreshToken = it },
        clearRefreshToken = { storedRefreshToken = null },
      ),
      loginWithGoogle = { tokens("signed-in") },
      reissue = { error("예상하지 않은 재발급") },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )

    viewModel.requireAuthentication { resumedWith = it }
    googleResult?.invoke("google-id-token", null)
    runCurrent()

    assertEquals("access-signed-in", resumedWith)
    assertEquals("signed-in", storedRefreshToken)
    assertEquals("access-signed-in", viewModel.tokens.value?.accessToken)
    assertEquals(false, viewModel.appleSignInAvailable)
  }

  @Test
  fun `Apple 로그인 뒤 보류 작업을 새 Access Token으로 재개한다`() = runTest {
    var appleResult: ((AppleSignInCredential?, String?) -> Unit)? = null
    var receivedCredential: AppleSignInCredential? = null
    var resumedWith: String? = null
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { error("호출되면 안 됨") },
        readRefreshToken = { null },
        writeRefreshToken = {},
        clearRefreshToken = {},
        requestAppleCredential = { appleResult = it },
      ),
      loginWithGoogle = { error("호출되면 안 됨") },
      loginWithApple = {
        receivedCredential = it
        tokens("apple")
      },
      reissue = { error("호출되면 안 됨") },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    val credential = AppleSignInCredential("identity", "code", "nonce")

    viewModel.requireAppleAuthentication { resumedWith = it }
    appleResult?.invoke(credential, null)
    runCurrent()

    assertEquals(true, viewModel.appleSignInAvailable)
    assertEquals(credential, receivedCredential)
    assertEquals("access-apple", resumedWith)
  }

  @Test
  fun `플랫폼 로그인이 취소되면 보류 작업을 실행하지 않는다`() = runTest {
    var googleResult: ((String?, String?) -> Unit)? = null
    var resumed = false
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { googleResult = it },
        readRefreshToken = { null },
        writeRefreshToken = {},
        clearRefreshToken = {},
      ),
      loginWithGoogle = { error("호출되면 안 됨") },
      reissue = { error("호출되면 안 됨") },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )

    viewModel.requireAuthentication { resumed = true }
    googleResult?.invoke(null, "로그인을 취소했어요.")
    advanceUntilIdle()

    assertEquals(false, resumed)
    assertEquals("로그인을 취소했어요.", viewModel.uiState.value.errorMessage)
    assertNull(viewModel.tokens.value)
  }

  private fun TestScope.authSession(backend: FakeAuthBackend): AuthSession = AuthSession(
    readRefreshToken = { backend.storedRefreshToken },
    writeRefreshToken = { backend.storedRefreshToken = it },
    clearRefreshToken = {
      backend.clearCount++
      backend.storedRefreshToken = null
    },
    reissue = backend::reissue,
    logout = backend::logout,
    scope = backgroundScope,
    currentTimeMillis = { testScheduler.currentTime },
  )

  private fun tokens(refreshToken: String) = MobileAuthTokens(
    accessToken = "access-$refreshToken",
    refreshToken = refreshToken,
    tokenType = "Bearer",
    accessTokenExpiresIn = 31,
    refreshTokenExpiresIn = 1_209_600,
  )

  private class FakeAuthBackend(
    var storedRefreshToken: String? = null,
    private val reissueResult: suspend (String) -> MobileAuthTokens = { error("예상하지 않은 재발급") },
  ) {
    val reissueRequests = mutableListOf<String>()
    val logoutRequests = mutableListOf<String>()
    var clearCount = 0

    suspend fun reissue(refreshToken: String): MobileAuthTokens {
      reissueRequests += refreshToken
      return reissueResult(refreshToken)
    }

    suspend fun logout(refreshToken: String) {
      logoutRequests += refreshToken
    }
  }

  private companion object {
    const val RENEWAL_INTERVAL_MILLIS = 1_000L
    const val RETRY_DELAY_MILLIS = 30_000L
  }
}
