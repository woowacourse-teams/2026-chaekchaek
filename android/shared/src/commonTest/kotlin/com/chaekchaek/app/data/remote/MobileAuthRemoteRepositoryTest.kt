package com.chaekchaek.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class MobileAuthRemoteRepositoryTest {
  @Test
  fun `게스트 토큰을 발급하고 저장 모델로 변환한다`() = runTest {
    var requestedMethod: HttpMethod? = null
    var requestedUrl: String? = null
    val engine = MockEngine { request ->
      requestedMethod = request.method
      requestedUrl = request.url.toString()
      respond(
        content = """{"guestToken":"guest-token","nickname":"다정한 파란 참새","expiresAt":"2026-09-25T09:00:00"}""",
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
      )
    }
    val repository = MobileAuthRemoteRepository(
      HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
      },
    )

    val guest = repository.issueGuest()

    assertEquals(HttpMethod.Post, requestedMethod)
    assertEquals("https://api.chaekchaek.com/api/v1/auth/guest-token", requestedUrl)
    assertEquals("guest-token", guest.token)
    assertEquals("다정한 파란 참새", guest.nickname)
    assertEquals("2026-09-25T09:00:00", guest.expiresAt)
  }

  @Test
  fun `모바일 로그인 응답 토큰을 역직렬화한다`() {
    val tokens = Json.decodeFromString<MobileAuthTokens>("""{"accessToken":"access","refreshToken":"refresh","tokenType":"Bearer","accessTokenExpiresIn":1800,"refreshTokenExpiresIn":1209600}""")

    assertEquals("access", tokens.accessToken)
    assertEquals(1_209_600, tokens.refreshTokenExpiresIn)
  }

  @Test
  fun `모바일 로그인 오류 코드를 역직렬화한다`() {
    val problem = Json.decodeFromString<MobileLoginProblem>("""{"code":"INVALID_GOOGLE_ID_TOKEN"}""")

    assertEquals("INVALID_GOOGLE_ID_TOKEN", problem.code)
  }

  @Test
  fun `재발급과 로그아웃 요청에 Refresh Token을 직렬화한다`() {
    val request = Json.encodeToString(RefreshTokenRequest("refresh"))

    assertEquals("{\"refreshToken\":\"refresh\"}", request)
  }
}
