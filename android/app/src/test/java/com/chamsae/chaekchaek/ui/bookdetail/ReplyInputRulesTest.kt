package com.chamsae.chaekchaek.ui.bookdetail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyInputRulesTest {
  @Test
  fun `공백과 200자 초과 답글은 제출할 수 없다`() {
    assertFalse(ReplyInputRules.canSubmit("  \n"))
    assertTrue(ReplyInputRules.canSubmit("가".repeat(200)))
    assertFalse(ReplyInputRules.canSubmit("가".repeat(201)))
  }
}
