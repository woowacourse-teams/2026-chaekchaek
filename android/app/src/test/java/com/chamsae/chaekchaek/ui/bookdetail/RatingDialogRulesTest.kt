package com.chamsae.chaekchaek.ui.bookdetail

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingDialogRulesTest {
  @Test
  fun `평균 별점만큼 각 별의 채움 비율을 계산한다`() {
    assertEquals(listOf(1f, 1f, 1f, 0.7f, 0f), averageRatingStarFillFractions(3.7))
    assertEquals(List(5) { 0f }, averageRatingStarFillFractions(null))
  }

  @Test
  fun `반쪽 슬롯을 별점과 설명으로 변환한다`() {
    assertEquals("0.5 · 아쉬워요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(0)))
    assertEquals("3.5 · 괜찮아요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(6)))
    assertEquals("5.0 · 최고예요", RatingDialogRules.label(RatingDialogRules.ratingAtSlot(9)))
  }
}
