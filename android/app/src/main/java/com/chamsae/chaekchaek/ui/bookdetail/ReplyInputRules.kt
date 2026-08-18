package com.chamsae.chaekchaek.ui.bookdetail

import com.chamsae.chaekchaek.data.ReflectionReply

internal object ReplyInputRules {
  const val MAX_LENGTH = ReflectionReply.MAX_LENGTH

  fun canSubmit(value: String): Boolean = value.isNotBlank() && value.length <= MAX_LENGTH
}
