package com.chamsae.chaekchaek.auth

import com.chaekchaek.app.data.remote.MobileAuthTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSession {
  private val _tokens = MutableStateFlow<MobileAuthTokens?>(null)
  val tokens: StateFlow<MobileAuthTokens?> = _tokens.asStateFlow()

  fun signIn(tokens: MobileAuthTokens) {
    _tokens.value = tokens
  }
}
