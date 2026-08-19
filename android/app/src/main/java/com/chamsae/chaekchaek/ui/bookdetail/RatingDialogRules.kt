package com.chamsae.chaekchaek.ui.bookdetail

import com.chaekchaek.app.domain.rating.Rating
import java.util.Locale

internal object RatingDialogRules {
  fun ratingAtSlot(slot: Int): Rating = Rating.ofHalfStars(slot + 1)

  fun label(rating: Rating): String =
    String.format(Locale.KOREA, "%.1f · %s", rating.score, description(rating))

  private fun description(rating: Rating): String =
    when (rating.score.toInt()) {
      0, 1 -> "아쉬워요"
      2 -> "그저 그래요"
      3 -> "괜찮아요"
      4 -> "좋았어요"
      else -> "최고예요"
    }
}
