package com.chaekchaek.app.analytics

import kotlin.random.Random
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode

data class AnalyticsEvent(
    val name: String,
    val stringParameters: Map<String, String>,
    val longParameters: Map<String, Long>,
    val doubleParameters: Map<String, Double>,
)

class AnalyticsTracker(
    private val environment: String,
    private val sink: (AnalyticsEvent) -> Unit,
) {
    private val runId = newId("run")
    private var eventSequence = 0L
    private var signedIn = false
    private var currentFlowId: String? = null
    private var currentOrigin: String? = null
    private var currentViewId: String? = null
    private var currentSearchId: String? = null
    private var currentActionId: String? = null
    private var currentAttempt: AnalyticsAttempt? = null
    private var currentComposer: AnalyticsComposer? = null
    private val recordedImpressions = mutableSetOf<String>()
    private val lastBookImpression = mutableMapOf<String, BookImpressionContext>()

    fun updateAuthentication(signedIn: Boolean) {
        this.signedIn = signedIn
    }

    fun startFlow(origin: String): String {
        currentFlowId = newId("flow")
        currentOrigin = origin
        if (origin != "search") currentSearchId = null
        return requireNotNull(currentFlowId)
    }

    fun currentFlow(): String? = currentFlowId

    fun ensureFlow(origin: String): String = currentFlowId ?: startFlow(origin)

    fun startSearch(): String {
        currentSearchId = newId("search")
        resetImpressionScope()
        return requireNotNull(currentSearchId)
    }

    fun newId(prefix: String): String = "$prefix-${Random.nextLong().toULong().toString(16)}"

    fun log(
        name: String,
        strings: Map<String, String> = emptyMap(),
        longs: Map<String, Long> = emptyMap(),
        doubles: Map<String, Double> = emptyMap(),
    ) {
        eventSequence += 1
        val contextualStrings = buildMap {
            put("environment", environment)
            put("auth_state", if (signedIn) "signed_in" else "signed_out")
            put("event_id", newId("event"))
            put("run_id", runId)
            currentFlowId?.let { put("flow_id", it) }
            currentOrigin?.let { put("origin", it) }
            currentViewId?.let { put("view_id", it) }
            currentSearchId?.let { put("search_id", it) }
            currentActionId?.let { put("action_id", it) }
            currentComposer?.let {
                put("composer_id", it.id)
                put("content_type", it.contentType)
                put("composer_mode", it.mode)
            }
            strings.forEach { (key, value) -> put(key, value.take(MAX_STRING_LENGTH)) }
        }
        val event = AnalyticsEvent(
            name = name,
            stringParameters = contextualStrings,
            longParameters = mapOf(
                "schema_version" to SCHEMA_VERSION,
                "event_seq" to eventSequence,
            ) + longs,
            doubleParameters = doubles,
        )
        if (event.parameterCount <= MAX_PARAMETER_COUNT) runCatching { sink(event) }
    }

    fun screenView(screenName: String, previousScreen: String? = null): String {
        val viewId = newId("view")
        currentViewId = viewId
        resetImpressionScope()
        log(
            name = "screen_view",
            strings = buildMap {
                put("screen_name", screenName)
                put("screen_class", screenName)
                put("view_id", viewId)
                previousScreen?.let { put("previous_screen", it) }
            },
        )
        return viewId
    }

    fun contentImpression(
        contentType: String,
        contentId: String,
        bookKey: String?,
        listId: String,
        position: Int,
        isOwn: Boolean? = null,
    ) {
        val deduplicationKey = listOf(currentViewId, listId, contentType, contentId).joinToString("|")
        if (!recordedImpressions.add(deduplicationKey)) return
        if (bookKey != null) {
            lastBookImpression[bookKey] = BookImpressionContext(
                listId = listId,
                position = position,
                viewId = currentViewId,
                searchId = currentSearchId,
            )
        }
        log(
            name = "cc_content_impression",
            strings = buildMap {
                put("content_type", contentType)
                put("content_id", contentId)
                put("list_id", listId)
                bookKey?.let { put("book_key", it) }
                isOwn?.let { put("is_own", it.toString()) }
            },
            longs = mapOf("position" to position.toLong()),
        )
    }

    fun bookSelect(bookKey: String?) {
        val impression = bookKey?.let(lastBookImpression::get)?.takeIf {
            it.viewId == currentViewId && it.searchId == currentSearchId
        }
        log(
            name = "cc_book_select",
            strings = buildMap {
                bookKey?.let { put("book_key", it) }
                impression?.let { put("list_id", it.listId) }
                put("selection_state", "selected")
            },
            longs = impression?.let { mapOf("position" to it.position.toLong()) }.orEmpty(),
        )
    }

    fun startAction(
        action: String,
        strings: Map<String, String> = emptyMap(),
        longs: Map<String, Long> = emptyMap(),
    ): AnalyticsAction {
        val handle = AnalyticsAction(newId("action"), action, TimeSource.Monotonic.markNow())
        currentActionId = handle.id
        log(
            name = "cc_action_start",
            strings = mapOf("action_id" to handle.id, "action" to action) + strings,
            longs = longs,
        )
        return handle
    }

    fun finishAction(
        handle: AnalyticsAction,
        outcome: String,
        strings: Map<String, String> = emptyMap(),
        longs: Map<String, Long> = emptyMap(),
        doubles: Map<String, Double> = emptyMap(),
    ) {
        log(
            name = "cc_action_result",
            strings = mapOf(
                "action_id" to handle.id,
                "action" to handle.action,
                "outcome" to outcome,
            ) + strings,
            longs = mapOf(
                "duration_ms" to handle.started.elapsedNow().inWholeMilliseconds,
                "attempt_count" to 1L,
            ) + longs,
            doubles = doubles,
        )
        if (currentActionId == handle.id) currentActionId = null
    }

    fun beginAuthAttempt(provider: String, trigger: String): String {
        val attempt = AnalyticsAttempt(newId("attempt"), provider, trigger, TimeSource.Monotonic.markNow())
        currentAttempt = attempt
        log(
            name = "cc_au" + "th_start",
            strings = mapOf(
                "au" + "th_attempt_id" to attempt.id,
                "method" to provider,
                "trigger_action" to trigger,
            ),
        )
        return attempt.id
    }

    fun endAuthAttempt(outcome: String, errorCategory: String? = null) {
        val attempt = currentAttempt ?: return
        log(
            name = "cc_au" + "th_result",
            strings = buildMap {
                put("au" + "th_attempt_id", attempt.id)
                put("method", attempt.provider)
                put("trigger_action", attempt.trigger)
                put("outcome", outcome)
                errorCategory?.let { put("error_category", it) }
            },
            longs = mapOf("duration_ms" to attempt.started.elapsedNow().inWholeMilliseconds),
        )
        currentAttempt = null
    }

    fun openComposer(contentType: String, mode: String, bookKey: String? = null): String {
        val composer = AnalyticsComposer(newId("composer"), contentType, mode)
        currentComposer = composer
        log(
            name = "cc_composer_open",
            strings = buildMap {
                put("composer_id", composer.id)
                put("content_type", contentType)
                put("composer_mode", mode)
                bookKey?.let { put("book_key", it) }
            },
        )
        return composer.id
    }

    fun dismissComposer(hasInput: Boolean = false) {
        if (currentComposer == null) return
        log(
            name = "cc_composer_dismiss",
            strings = mapOf("has_input" to hasInput.toString()),
        )
        currentComposer = null
    }

    fun completeComposer() {
        currentComposer = null
    }

    private fun resetImpressionScope() {
        recordedImpressions.clear()
        lastBookImpression.clear()
    }

    companion object {
        val None = AnalyticsTracker("test") {}
        private const val SCHEMA_VERSION = 1L
        private const val MAX_PARAMETER_COUNT = 25
        private const val MAX_STRING_LENGTH = 100
    }
}

class AnalyticsAction internal constructor(
    val id: String,
    val action: String,
    internal val started: TimeMark,
)

private data class AnalyticsAttempt(
    val id: String,
    val provider: String,
    val trigger: String,
    val started: TimeMark,
)

private data class AnalyticsComposer(
    val id: String,
    val contentType: String,
    val mode: String,
)

private data class BookImpressionContext(
    val listId: String,
    val position: Int,
    val viewId: String?,
    val searchId: String?,
)

fun analyticsBookKey(isbn13: String?, catalogId: String?): String? = when {
    !isbn13.isNullOrBlank() -> "isbn13:${isbn13.filter(Char::isDigit)}"
    !catalogId.isNullOrBlank() -> "catalog:$catalogId"
    else -> null
}

fun analyticsErrorCategory(error: Throwable): String = when {
    error is ResponseException && error.response.status == HttpStatusCode.Unauthorized -> "auth"
    error is ResponseException && error.response.status == HttpStatusCode.NotFound -> "not_found"
    error is ResponseException && error.response.status == HttpStatusCode.Conflict -> "conflict"
    error is ResponseException && error.response.status.value >= 500 -> "server"
    error::class.simpleName?.contains("timeout", ignoreCase = true) == true -> "timeout"
    error::class.simpleName?.contains("connect", ignoreCase = true) == true -> "network"
    else -> "unknown"
}

fun analyticsQueryLengthBucket(length: Int): String = when (length) {
    in 1..5 -> "1_5"
    in 6..15 -> "6_15"
    else -> "16_plus"
}

private val AnalyticsEvent.parameterCount: Int
    get() = stringParameters.size + longParameters.size + doubleParameters.size
