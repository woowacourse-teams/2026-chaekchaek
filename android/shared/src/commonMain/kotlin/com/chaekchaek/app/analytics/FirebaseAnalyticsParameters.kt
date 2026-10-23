package com.chaekchaek.app.analytics

/** Firebase의 파라미터 제한을 제공자 어댑터에서만 적용한다. PostHog 원본은 유지한다. */
fun firebaseAnalyticsEvent(event: AnalyticsEvent): AnalyticsEvent {
    val keys = event.stringParameters.keys + event.longParameters.keys + event.doubleParameters.keys
    val selected = (PRIORITY_KEYS.filter { it in keys } + keys.sorted()).distinct().take(25).toSet()
    return event.copy(
        stringParameters = event.stringParameters.filterKeys { it in selected },
        longParameters = event.longParameters.filterKeys { it in selected },
        doubleParameters = event.doubleParameters.filterKeys { it in selected },
    )
}

private val PRIORITY_KEYS = listOf(
    "event_id", "schema_version", "environment", "run_id", "event_seq",
    "action_id", "action", "outcome", "effect", "error_category", "duration_ms",
    "collection_revision", "sample_rate", "screen_name", "view_id", "flow_id", "origin",
    "search_id", "composer_id", "auth_attempt_id", "book_key", "content_id", "list_id", "position",
    "result_count", "resource", "trigger", "auth_state", "rating", "control", "old_value", "new_value",
)
