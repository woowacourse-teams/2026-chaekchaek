package com.chaekchaek.app.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
  @Test
  fun `재발급 중의 null은 인증 복원 중이며 확정 로그아웃과 구분한다`() = runTest {
    val restored = CompletableDeferred<MobileAuthTokens>()
    val session = AuthSession(
      readRefreshToken = { "stored" },
      writeRefreshToken = {},
      clearRefreshToken = {},
      reissue = { restored.await() },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    runCurrent()
    assertNull(session.tokens.value)
    assertEquals(true, session.isRestoring.value)

    restored.complete(tokens("restored"))
    runCurrent()
    assertEquals(false, session.isRestoring.value)
    assertEquals("restored", session.tokens.value?.refreshToken)

    session.signOut()
    assertEquals(false, session.isRestoring.value)
    assertNull(session.tokens.value)
  }

  @Test
  fun `영구 인증 실패는 복원을 종료하고 비회원 상태를 확정한다`() = runTest {
    val session = AuthSession(
      readRefreshToken = { "expired" },
      writeRefreshToken = {},
      clearRefreshToken = {},
      reissue = { throw MobileLoginException("INVALID_REFRESH_TOKEN", 401) },
      logout = {},
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    runCurrent()
    assertEquals(false, session.isRestoring.value)
    assertNull(session.tokens.value)
  }

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
  fun `홈 재시도는 Access Token을 즉시 재발급한다`() = runTest {
    val backend = FakeAuthBackend { tokens("refreshed") }
    val session = authSession(backend)
    session.signIn(tokens("expired"))

    val accessToken = session.refreshAccessTokenForRetry()

    assertEquals("access-refreshed", accessToken)
    assertEquals(listOf("expired"), backend.reissueRequests)
    assertEquals("refreshed", backend.storedRefreshToken)
  }

  @Test
  fun `동시에 홈을 재시도하면 Token 재발급을 공유한다`() = runTest {
    val reissueStarted = CompletableDeferred<Unit>()
    val finishReissue = CompletableDeferred<Unit>()
    val backend = FakeAuthBackend {
      reissueStarted.complete(Unit)
      finishReissue.await()
      tokens("refreshed")
    }
    val session = authSession(backend)
    session.signIn(tokens("expired"))

    val first = launch { assertEquals("access-refreshed", session.refreshAccessTokenForRetry()) }
    reissueStarted.await()
    val second = launch { assertEquals("access-refreshed", session.refreshAccessTokenForRetry()) }
    runCurrent()
    assertEquals(listOf("expired"), backend.reissueRequests)

    finishReissue.complete(Unit)
    first.join()
    second.join()
    assertEquals(listOf("expired"), backend.reissueRequests)
  }

  @Test
  fun `일시적인 재발급 실패 후 홈을 다시 시도할 수 있다`() = runTest {
    var attempts = 0
    val backend = FakeAuthBackend {
      if (attempts++ == 0) throw MobileLoginException("INTERNAL_SERVER_ERROR", 500)
      tokens("refreshed")
    }
    val session = authSession(backend)
    session.signIn(tokens("expired"))

    assertFailsWith<MobileLoginException> { session.refreshAccessTokenForRetry() }
    assertEquals("expired", backend.storedRefreshToken)
    assertEquals("access-refreshed", session.refreshAccessTokenForRetry())
    assertEquals(listOf("expired", "expired"), backend.reissueRequests)
  }

  @Test
  fun `만료된 Refresh Token은 홈 재시도에서 세션을 정리한다`() = runTest {
    val backend = FakeAuthBackend {
      throw MobileLoginException("INVALID_REFRESH_TOKEN", 401)
    }
    val session = authSession(backend)
    session.signIn(tokens("expired"))

    assertNull(session.refreshAccessTokenForRetry())

    assertNull(session.tokens.value)
    assertNull(backend.storedRefreshToken)
    assertEquals(1, backend.clearCount)
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

    assertEquals(0, backend.clearCount)
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
    assertEquals(true, session.isRestoring.value)

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
    assertEquals(true, session.isRestoring.value)

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
  fun `로그아웃은 서버 응답을 기다리지 않고 로컬 세션을 지운다`() = runTest {
    val logoutStarted = CompletableDeferred<Unit>()
    val finishLogout = CompletableDeferred<Unit>()
    var storedRefreshToken: String? = null
    val session = AuthSession(
      readRefreshToken = { storedRefreshToken },
      writeRefreshToken = { storedRefreshToken = it },
      clearRefreshToken = { storedRefreshToken = null },
      reissue = { error("예상하지 않은 재발급") },
      logout = {
        logoutStarted.complete(Unit)
        finishLogout.await()
      },
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    session.signIn(tokens("signed-in"))
    val localSessionCleared = CompletableDeferred<Unit>()

    val signOut = launch { session.signOut { localSessionCleared.complete(Unit) } }
    localSessionCleared.await()

    assertNull(session.tokens.value)
    assertNull(storedRefreshToken)
    logoutStarted.await()
    assertEquals(false, signOut.isCompleted)

    finishLogout.complete(Unit)
    signOut.join()
  }

  @Test
  fun `원격 로그아웃 실패에도 로컬 세션은 로그아웃 상태를 유지한다`() = runTest {
    var storedRefreshToken: String? = null
    val session = AuthSession(
      readRefreshToken = { storedRefreshToken },
      writeRefreshToken = { storedRefreshToken = it },
      clearRefreshToken = { storedRefreshToken = null },
      reissue = { error("예상하지 않은 재발급") },
      logout = { error("원격 로그아웃 실패") },
      scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    session.signIn(tokens("signed-in"))

    session.signOut()

    assertNull(session.tokens.value)
    assertNull(storedRefreshToken)
  }

  @Test
  fun `로그인 뒤 보류 작업을 새 Access Token으로 재개한다`() = runTest {
    var googleResult: ((String?, String?) -> Unit)? = null
    var storedRefreshToken: String? = null
    var resumedWith: String? = null
    var guest: GuestAuth? = GuestAuth("guest-token", "게스트", "2026-09-25T09:00:00")
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { googleResult = it },
        readRefreshToken = { storedRefreshToken },
        writeRefreshToken = { storedRefreshToken = it },
        clearRefreshToken = { storedRefreshToken = null },
        readGuest = { guest },
        clearGuest = { guest = null },
      ),
      loginWithGoogle = { idToken, guestToken ->
        assertEquals("google-id-token", idToken)
        assertEquals("guest-token", guestToken)
        tokens("signed-in")
      },
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
    assertNull(guest)
  }

  @Test
  fun `Apple 로그인 뒤 보류 작업을 새 Access Token으로 재개한다`() = runTest {
    var appleResult: ((AppleSignInCredential?, String?) -> Unit)? = null
    var receivedCredential: AppleSignInCredential? = null
    var resumedWith: String? = null
    var guest: GuestAuth? = GuestAuth("guest-token", "게스트", "2026-09-25T09:00:00")
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { error("호출되면 안 됨") },
        readRefreshToken = { null },
        writeRefreshToken = {},
        clearRefreshToken = {},
        requestAppleCredential = { appleResult = it },
        readGuest = { guest },
        clearGuest = { guest = null },
      ),
      loginWithGoogle = { _, _ -> error("호출되면 안 됨") },
      loginWithApple = { received, guestToken ->
        receivedCredential = received
        assertEquals("guest-token", guestToken)
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
    assertNull(guest)
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
      loginWithGoogle = { _, _ -> error("호출되면 안 됨") },
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

  @Test
  fun `재발급 응답을 기다리지 않고 로그아웃하고 늦은 응답은 폐기한다`() = runTest {
    val response = CompletableDeferred<MobileAuthTokens>()
    var stored: String? = null
    var localCleared = false
    val session = AuthSession(
      readRefreshToken = { stored }, writeRefreshToken = { stored = it },
      clearRefreshToken = { stored = null },
      reissue = { withContext(NonCancellable) { response.await() } },
      logout = {}, scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    runCurrent()
    session.signIn(tokens("signed-in"))
    backgroundScope.launch { session.refreshAccessTokenForRetry() }
    runCurrent()
    backgroundScope.launch { session.signOut { localCleared = true } }
    runCurrent()
    assertEquals(true, localCleared)
    assertNull(session.tokens.value)
    assertNull(stored)
    response.complete(tokens("late"))
    runCurrent()
    assertNull(session.tokens.value)
    assertNull(stored)
  }

  @Test
  fun `한 호출자의 취소는 공유 재발급을 분리하지 않는다`() = runTest {
    val response = CompletableDeferred<MobileAuthTokens>()
    val backend = FakeAuthBackend { response.await() }
    val session = authSession(backend)
    runCurrent()
    session.signIn(tokens("signed-in"))
    val canceled = backgroundScope.launch { session.refreshAccessTokenForRetry() }
    runCurrent()
    canceled.cancel()
    runCurrent()
    var received: String? = null
    backgroundScope.launch { received = session.refreshAccessTokenForRetry() }
    runCurrent()
    assertEquals(1, backend.reissueRequests.size)
    response.complete(tokens("renewed"))
    runCurrent()
    assertEquals("access-renewed", received)
  }

  @Test
  fun `유일한 호출자 취소 뒤 완료된 실패는 다음 재발급을 막지 않는다`() = runTest {
    val response = CompletableDeferred<MobileAuthTokens>()
    var attempts = 0
    val backend = FakeAuthBackend {
      attempts += 1
      if (attempts == 1) response.await() else tokens("renewed")
    }
    val session = authSession(backend)
    runCurrent()
    session.signIn(tokens("signed-in"))
    val canceled = backgroundScope.launch { session.refreshAccessTokenForRetry() }
    runCurrent()
    canceled.cancel()
    runCurrent()
    response.completeExceptionally(MobileLoginException("TEMPORARY", 500))
    runCurrent()

    var received: String? = null
    backgroundScope.launch { received = session.refreshAccessTokenForRetry() }
    runCurrent()
    assertEquals(2, attempts)
    assertEquals("access-renewed", received)
  }

  @Test
  fun `건너뛰기 뒤 늦은 로그인 응답은 세션과 게스트를 변경하지 않는다`() = runTest {
    var credentialResult: ((String?, String?) -> Unit)? = null
    val response = CompletableDeferred<MobileAuthTokens>()
    var stored: String? = null
    var guestCleared = false
    var resumed = false
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { credentialResult = it },
        readRefreshToken = { stored },
        writeRefreshToken = { stored = it },
        clearRefreshToken = { stored = null },
        clearGuest = { guestCleared = true },
      ),
      loginWithGoogle = { _, _ -> withContext(NonCancellable) { response.await() } },
      reissue = { error("예상하지 않은 재발급") },
      logout = {}, scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    viewModel.requireAuthentication { resumed = true }
    credentialResult!!.invoke("credential", null)
    runCurrent()
    viewModel.cancelPendingAuthentication()
    response.complete(tokens("late"))
    runCurrent()
    assertNull(viewModel.tokens.value)
    assertNull(stored)
    assertEquals(false, guestCleared)
    assertEquals(false, resumed)
    assertEquals(AuthUiState(), viewModel.uiState.value)
  }

  @Test
  fun `이전 공급업체 응답이 새 로그인 요청을 변경하지 않는다`() = runTest {
    val callbacks = mutableListOf<(String?, String?) -> Unit>()
    var loginCalls = 0
    val viewModel = AuthViewModel(
      callbacks = AuthPlatformCallbacks(
        requestGoogleIdToken = { callbacks += it }, readRefreshToken = { null },
        writeRefreshToken = {}, clearRefreshToken = {},
      ),
      loginWithGoogle = { _, _ -> loginCalls++; tokens("new") },
      reissue = { error("예상하지 않은 재발급") }, logout = {}, scope = backgroundScope,
      currentTimeMillis = { testScheduler.currentTime },
    )
    viewModel.requireAuthentication {}
    viewModel.cancelPendingAuthentication()
    viewModel.requireAuthentication {}
    callbacks[0]("old", null)
    runCurrent()
    assertEquals(0, loginCalls)
    assertEquals(true, viewModel.uiState.value.signingIn)
    callbacks[1]("new", null)
    runCurrent()
    assertEquals(1, loginCalls)
    assertEquals("access-new", viewModel.tokens.value?.accessToken)
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
