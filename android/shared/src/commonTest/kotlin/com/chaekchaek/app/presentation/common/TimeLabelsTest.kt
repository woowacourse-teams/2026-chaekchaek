package com.chaekchaek.app.presentation.common

import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimeLabelsTest {
    private val now = Instant.parse("2026-08-12T12:00:00Z")

    @Test
    fun `분 경계값을 상대 시각으로 표시한다`() {
        // given : 현재 시각에서 59초, 1분, 59분 전인 시각이 있다
        // when & then : 1분 미만은 방금, 이후는 지난 분으로 표시한다
        TimeLabels.relative(now - 59.seconds, now) shouldBe "방금"
        TimeLabels.relative(now - 1.minutes, now) shouldBe "1분 전"
        TimeLabels.relative(now - 59.minutes, now) shouldBe "59분 전"
    }

    @Test
    fun `시간과 일 경계값을 상대 시각으로 표시한다`() {
        // given : 시간과 일 단위의 경계 시각이 있다
        // when & then : 60분부터 시간, 24시간부터 일로 표시한다
        TimeLabels.relative(now - 1.hours, now) shouldBe "1시간 전"
        TimeLabels.relative(now - 23.hours, now) shouldBe "23시간 전"
        TimeLabels.relative(now - 1.days, now) shouldBe "1일 전"
        TimeLabels.relative(now - 6.days, now) shouldBe "6일 전"
    }

    @Test
    fun `일주일 이상은 날짜로 표시하고 미래는 방금으로 처리한다`() {
        // given : 일주일 전과 미래 시각이 있다
        val old = now - 7.days
        val date = old.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val expectedDate =
            "${date.year}.${(date.month.ordinal + 1).twoDigits()}.${date.day.twoDigits()}"

        // when & then : 일주일 전은 날짜, 미래는 방금으로 표시한다
        TimeLabels.relative(old, now) shouldBe expectedDate
        TimeLabels.relative(now + 1.minutes, now) shouldBe "방금"
    }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')
}
