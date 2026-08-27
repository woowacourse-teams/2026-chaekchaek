package com.chaekchaek.app.data.remote

import com.chaekchaek.app.auth.GuestAuth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class MobileAuthRemoteRepository(
  private val client: HttpClient = createHttpClient(),
) {

  suspend fun loginWithGoogle(idToken: String): MobileAuthTokens =
    requestTokens("google", GoogleLoginRequest(idToken))

  suspend fun issueGuest(): GuestAuth =
    try {
      client.post("$BASE_URL/api/v1/auth/guest-token")
        .body<GuestAuthResponse>()
        .toGuestAuth()
    } catch (error: ResponseException) {
      throw error.toMobileLoginException()
    }

  suspend fun reissue(refreshToken: String): MobileAuthTokens =
    requestTokens("reissue", RefreshTokenRequest(refreshToken))

  suspend fun logout(refreshToken: String) {
    try {
      client.post("$BASE_URL/api/v1/auth/mobile/logout") {
        contentType(ContentType.Application.Json)
        setBody(RefreshTokenRequest(refreshToken))
      }
    } catch (error: ResponseException) {
      throw error.toMobileLoginException()
    }
  }

  private suspend fun requestTokens(path: String, body: Any): MobileAuthTokens =
    try {
      client.post("$BASE_URL/api/v1/auth/mobile/$path") {
        contentType(ContentType.Application.Json)
        setBody(body)
      }.body<MobileAuthTokens>()
    } catch (error: ResponseException) {
      throw error.toMobileLoginException()
    }

  private suspend fun ResponseException.toMobileLoginException(): MobileLoginException {
    val code = runCatching { response.body<MobileLoginProblem>().code }
      .getOrDefault("HTTP_${response.status.value}")
    return MobileLoginException(code, response.status.value)
  }

  private companion object {
    const val BASE_URL = "https://api.chaekchaek.com"
  }
}

@Serializable
private data class GoogleLoginRequest(val idToken: String)

@Serializable
internal data class GuestAuthResponse(
  val guestToken: String,
  val nickname: String,
  val expiresAt: String,
)

internal fun GuestAuthResponse.toGuestAuth() = GuestAuth(guestToken, nickname, expiresAt)

@Serializable
internal data class RefreshTokenRequest(val refreshToken: String)

@Serializable
internal data class MobileLoginProblem(val code: String)

class MobileLoginException(val code: String, val statusCode: Int) : RuntimeException(code)

@Serializable
data class MobileAuthTokens(
  val accessToken: String,
  val refreshToken: String,
  val tokenType: String,
  val accessTokenExpiresIn: Long,
  val refreshTokenExpiresIn: Long,
)
