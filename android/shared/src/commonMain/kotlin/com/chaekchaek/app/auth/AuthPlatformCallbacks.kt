package com.chaekchaek.app.auth

class AuthPlatformCallbacks(
  val requestGoogleIdToken: (onResult: (idToken: String?, errorMessage: String?) -> Unit) -> Unit,
  val readRefreshToken: () -> String?,
  val writeRefreshToken: (String) -> Unit,
  val clearRefreshToken: () -> Unit,
  val readGuest: () -> GuestAuth? = { null },
  val writeGuest: (GuestAuth) -> Unit = {},
  val clearGuest: () -> Unit = {},
)

data class GuestAuth(
  val token: String,
  val nickname: String,
  val expiresAt: String,
)
