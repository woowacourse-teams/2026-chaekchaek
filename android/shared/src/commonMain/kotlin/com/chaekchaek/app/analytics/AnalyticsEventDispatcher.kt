package com.chaekchaek.app.analytics

/** 각 분석 제공자의 전송 실패가 다른 제공자의 전송을 막지 않게 한다. */
class AnalyticsEventDispatcher(
    private val primary: (AnalyticsEvent) -> Unit,
    private val secondary: (AnalyticsEvent) -> Unit,
) {
    fun log(event: AnalyticsEvent) {
        runCatching { primary(event) }
        runCatching { secondary(event) }
    }
}
