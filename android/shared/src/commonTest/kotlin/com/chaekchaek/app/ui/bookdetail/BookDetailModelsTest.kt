package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.domain.rating.Rating
import kotlin.test.Test
import kotlin.test.assertEquals

class BookDetailModelsTest {
    @Test
    fun `최근 별점은 같은 책을 갱신하고 3건만 유지한다`() {
        val ratings = listOf("a", "b", "c").map {
            RatedBookUiModel(it, it, Rating.ofHalfStars(2), "이전")
        }

        val updated = ratings.withRecentRating("b", "B", Rating.ofHalfStars(8), "방금")
            .withRecentRating("d", "D", Rating.ofHalfStars(10), "방금")

        assertEquals(listOf("c", "b", "d"), updated.map(RatedBookUiModel::bookId))
        assertEquals(8, updated[1].rating.halfStars)
    }
}
