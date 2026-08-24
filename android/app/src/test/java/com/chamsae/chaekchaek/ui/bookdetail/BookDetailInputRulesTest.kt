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

  @Test
  fun `감상 쪽수가 읽은 범위를 넘고 전체 공개하지 않았을 때만 잠근다`() {
    assertFalse(shouldLockReview(currentPage = 80, reviewPage = 80, spoilersRevealed = false))
    assertTrue(shouldLockReview(currentPage = 80, reviewPage = 160, spoilersRevealed = false))
    assertFalse(shouldLockReview(currentPage = 80, reviewPage = 160, spoilersRevealed = true))
  }
}
