package com.chaekchaek.app.auth

class AuthPlatformCallbacks(
  val requestGoogleIdToken: (onResult: (idToken: String?, errorMessage: String?) -> Unit) -> Unit,
  val readRefreshToken: () -> String?,
  val writeRefreshToken: (String) -> Unit,
  val clearRefreshToken: () -> Unit,
)
