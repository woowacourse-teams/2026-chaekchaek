package com.chamsae.chaekchaek.ui.bookdetail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookDetailInputRulesTest {
  @Test
  fun `감상은 내용이 있고 쪽수가 전체 범위 안일 때 제출할 수 있다`() {
    assertTrue(BookDetailInputRules.canSubmitReview("좋았다", "80", 308))
    assertTrue(BookDetailInputRules.canSubmitReview("좋았다", "", 308))
    assertFalse(BookDetailInputRules.canSubmitReview("  ", "80", 308))
    assertFalse(BookDetailInputRules.canSubmitReview("좋았다", "309", 308))
  }
}
