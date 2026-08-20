package com.chamsae.chaekchaek.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthSessionTest {
  @Test
  fun `Access Token은 만료 30초 전에 재발급한다`() {
    assertEquals(1L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 69_999L))
    assertEquals(0L, AuthSession.renewalDelayMillis(expiresAtMillis = 100_000L, currentTimeMillis = 70_000L))
  }
}
