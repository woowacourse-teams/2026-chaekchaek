package com.chaekchaek.app.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class MobileAuthRemoteRepositoryTest {
  @Test
  fun `모바일 로그인 응답 토큰을 역직렬화한다`() {
    val tokens = Json.decodeFromString<MobileAuthTokens>("""{"accessToken":"access","refreshToken":"refresh","tokenType":"Bearer","accessTokenExpiresIn":1800,"refreshTokenExpiresIn":1209600}""")

    assertEquals("access", tokens.accessToken)
    assertEquals(1_209_600, tokens.refreshTokenExpiresIn)
  }
}
