package com.chaekchaek.app.domain.rating

import kotlin.jvm.JvmInline

/**
 * 내가 매긴 별점. 0.5 단위로만 매길 수 있어 반개 단위 정수로 보관한다.
 *
 * Float 로 두면 `3.5f == 3.5f` 같은 부동소수점 동등 비교에 기대게 되고, 0.5 단위가 아닌 값도
 * 만들어진다. 계산된 평균 별점([RatingSummary])과 섞이지 않게 타입도 분리한다.
 */
@JvmInline
value class Rating private constructor(val halfStars: Int) : Comparable<Rating> {
    init {
        require(halfStars in MIN_HALF_STARS..MAX_HALF_STARS) {
            "별점은 ${MIN_SCORE}부터 ${MAX_SCORE}까지 0.5 단위입니다: ${halfStars / SCORE_UNIT}"
        }
    }

    val score: Float get() = halfStars / SCORE_UNIT

    override fun compareTo(other: Rating): Int = halfStars.compareTo(other.halfStars)

    companion object {
        private const val MIN_HALF_STARS = 1
        private const val MAX_HALF_STARS = 10
        private const val SCORE_UNIT = 2f

        const val MIN_SCORE = 0.5f
        const val MAX_SCORE = 5.0f

        fun ofHalfStars(halfStars: Int): Rating = Rating(halfStars)

        fun ofScore(score: Float): Rating {
            val halfStars = (score * SCORE_UNIT).toInt()
            require(halfStars / SCORE_UNIT == score) { "별점은 0.5 단위여야 합니다: $score" }
            return Rating(halfStars)
        }
    }
}

/**
 * 여러 사람의 별점을 집계한 결과. 계산된 값이라 0.5 단위가 아니고 직접 매길 수 없다.
 */
class RatingSummary(
    val average: Float,
    val raterCount: Int,
) {
    init {
        require(average in 0f..Rating.MAX_SCORE) { "평균 별점 범위를 벗어났습니다: $average" }
        require(raterCount >= 0) { "평점 인원은 0 이상이어야 합니다: $raterCount" }
    }

    fun hasRating(): Boolean = raterCount > 0
}
