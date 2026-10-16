package com.chaekchaek.app.presentation.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object TimeLabels {
    fun relative(at: Instant, now: Instant): String {
        if (at >= now) return "방금"

        val elapsed = now - at
        return when {
            elapsed < 1.minutes -> "방금"
            elapsed < 1.hours -> "${elapsed.inWholeMinutes}분 전"
            elapsed < 1.days -> "${elapsed.inWholeHours}시간 전"
            elapsed < 7.days -> "${elapsed.inWholeDays}일 전"
            else -> at.dateLabel()
        }
    }

    private fun Instant.dateLabel(): String {
        val date = toLocalDateTime(TimeZone.currentSystemDefault()).date
        return "${date.year}.${(date.month.ordinal + 1).twoDigits()}.${date.day.twoDigits()}"
    }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')
}
