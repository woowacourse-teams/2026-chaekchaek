package com.chaekchaek.app.domain.rating

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RatingTest {
    @Test
    fun `반개 단위가 아닌 별점은 만들 수 없다`() {
        // given & when & then : 3.7점은 별 반개로 나눠지지 않는다
        shouldThrow<IllegalArgumentException> { Rating.ofScore(3.7f) }
    }

    @Test
    fun `반개 단위 별점은 만들 수 있다`() {
        // given & when & then : 3.5점은 별 일곱 개 반이다
        shouldNotThrowAny { Rating.ofScore(3.5f) }
    }

    @Test
    fun `범위를 벗어난 별점은 만들 수 없다`() {
        // given & when & then : 0점과 5.5점은 별점 범위 밖이다
        shouldThrow<IllegalArgumentException> { Rating.ofScore(0f) }
        shouldThrow<IllegalArgumentException> { Rating.ofScore(5.5f) }
    }

    @Test
    fun `같은 점수로 만든 별점은 서로 같다`() {
        // given : 같은 4.0점을 다른 방법으로 만든다
        val fromScore = Rating.ofScore(4.0f)
        val fromHalfStars = Rating.ofHalfStars(8)

        // when & then : 두 별점은 같다
        fromScore shouldBe fromHalfStars
    }

    @Test
    fun `별점을 점수로 읽을 수 있다`() {
        // given : 별 일곱 개 반이 주어진다
        val rating = Rating.ofHalfStars(7)

        // when & then : 3.5점으로 읽힌다
        rating.score shouldBe 3.5f
    }
}

class RatingSummaryTest {
    @Test
    fun `평균 별점은 반개 단위가 아니어도 된다`() {
        // given & when & then : 100명 평균 4.2점은 유효하다
        shouldNotThrowAny { RatingSummary(average = 4.2f, raterCount = 100) }
    }

    @Test
    fun `평균 별점이 5점을 넘으면 만들 수 없다`() {
        // given & when & then : 5.1점 평균은 나올 수 없다
        shouldThrow<IllegalArgumentException> { RatingSummary(average = 5.1f, raterCount = 100) }
    }

    @Test
    fun `아무도 평점을 매기지 않았는지 알려준다`() {
        // given : 평가자가 없는 책이 주어진다
        val summary = RatingSummary(average = 0f, raterCount = 0)

        // when & then : 별점이 없는 상태다
        summary.hasRating() shouldBe false
    }
}
