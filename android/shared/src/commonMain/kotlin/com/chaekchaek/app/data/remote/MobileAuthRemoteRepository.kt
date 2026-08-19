package com.chaekchaek.app.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class MobileAuthRemoteRepository {
  private val client = createHttpClient()

  suspend fun loginWithGoogle(idToken: String): MobileAuthTokens =
    try {
      client.post("$BASE_URL/api/v1/auth/mobile/google") {
        contentType(ContentType.Application.Json)
        setBody(GoogleLoginRequest(idToken))
      }.body<MobileAuthTokens>()
    } catch (error: ResponseException) {
      val code = runCatching { error.response.body<MobileLoginProblem>().code }
        .getOrDefault("HTTP_${error.response.status.value}")
      throw MobileLoginException(code)
    }

  private companion object {
    const val BASE_URL = "https://api.chaekchaek.com"
  }
}

@Serializable
private data class GoogleLoginRequest(val idToken: String)

@Serializable
internal data class MobileLoginProblem(val code: String)

class MobileLoginException(val code: String) : RuntimeException(code)

@Serializable
data class MobileAuthTokens(
  val accessToken: String,
  val refreshToken: String,
  val tokenType: String,
  val accessTokenExpiresIn: Long,
  val refreshTokenExpiresIn: Long,
)
