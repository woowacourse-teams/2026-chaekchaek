package com.chamsae.chaekchaek.ui.bookdetail

internal object ReplyInputRules {
  const val MAX_LENGTH = 200

  fun canSubmit(value: String): Boolean = value.isNotBlank() && value.length <= MAX_LENGTH
}
