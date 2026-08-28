package com.chaekchaek.app.data.remote

import io.ktor.http.HttpHeaders

sealed interface WriteCredential {
  val headerName: String
  val headerValue: String

  data class Member(val accessToken: String) : WriteCredential {
    override val headerName: String = HttpHeaders.Authorization
    override val headerValue: String = "Bearer $accessToken"
  }

  data class Guest(val guestToken: String) : WriteCredential {
    override val headerName: String = GUEST_TOKEN_HEADER
    override val headerValue: String = guestToken
  }

  companion object {
    const val GUEST_TOKEN_HEADER = "X-Guest-Token"
  }
}
