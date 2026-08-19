package com.chamsae.chaekchaek.ui.bookdetail

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingDialogRulesTest {
  @Test
  fun `반쪽 슬롯을 별점과 설명으로 변환한다`() {
    assertEquals("0.5 · 아쉬워요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(0)))
    assertEquals("3.5 · 괜찮아요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(6)))
    assertEquals("5.0 · 최고예요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(9)))
  }
}
