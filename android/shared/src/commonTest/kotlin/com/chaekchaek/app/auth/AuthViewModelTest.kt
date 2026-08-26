package com.chaekchaek.app.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

  private fun tokens(refreshToken: String) = MobileAuthTokens(
    accessToken = "access-$refreshToken",
    refreshToken = refreshToken,
    tokenType = "Bearer",
    accessTokenExpiresIn = 31,
    refreshTokenExpiresIn = 1_209_600,
  )
}
