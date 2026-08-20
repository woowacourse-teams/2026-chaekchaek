package com.chamsae.chaekchaek.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthSessionTest {
  @Test
  fun `Access Token은 만료 30초 전에 재발급한다`() {
    assertEquals(1L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 69_999L))
    assertEquals(0L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 70_000L))
  }

  @Test
  fun `저장한 Refresh Token으로 세션을 복원한다`() = runTest {
    val backend = FakeAuthBackend(storedRefreshToken = "stored") { tokens("rotated") }
    val session = authSession(backend)

    runCurrent()

    assertEquals(listOf("stored"), backend.reissueRequests)
    assertEquals("rotated", session.tokens.value?.refreshToken)
    assertEquals("rotated", backend.storedRefreshToken)
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
