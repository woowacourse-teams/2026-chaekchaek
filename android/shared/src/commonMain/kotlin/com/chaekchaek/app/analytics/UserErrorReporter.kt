package com.chaekchaek.app.analytics

import com.chaekchaek.app.data.remote.MobileLoginException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import kotlin.time.TimeSource

/** 처리된 실패의 분류, 개인정보 제거, 중복 및 폭주 방지를 담당한다. UI 스레드에서 호출한다. */
class UserErrorReporter(
    private val sink: (UserErrorDetails) -> Unit = {},
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val recent = ArrayDeque<Pair<Throwable, String>>()
    private var window = timeSource.markNow()
    private var sentInWindow = 0

    fun report(error: Throwable, operation: String) {
        if (error is CancellationException) return
        if (!operation.matches(Regex("[A-Za-z_]{1,64}"))) return
        if (recent.any { it.first === error && it.second == operation }) return
        recent.addLast(error to operation)
        if (recent.size > 64) recent.removeFirst()
        if (window.elapsedNow().inWholeSeconds >= 60) {
            window = timeSource.markNow()
            sentInWindow = 0
        }
        if (sentInWindow >= 20) return
        sentInWindow++
        val status = (error as? ResponseException)?.response?.status?.value
            ?: (error as? MobileLoginException)?.statusCode
        val category = when (status) {
            400, 422 -> "validation"
            401 -> "auth"
            403 -> "permission"
            else -> analyticsErrorCategory(error)
        }
        val details = UserErrorDetails(
            operation = operation,
            category = category,
            status = status,
            exceptionType = diagnosticSymbol(error::class.simpleName ?: "Throwable"),
            frames = diagnosticFrames(error),
        )
        runCatching { sink(details) }
    }

    companion object { val None = UserErrorReporter() }
}

class UserErrorDetails internal constructor(
    val operation: String,
    val category: String,
    val status: Int?,
    val exceptionType: String,
    internal val frames: List<DiagnosticFrame>,
) {
    internal fun properties(event: AnalyticsEvent): String = buildJsonObject {
        event.stringParameters.forEach { (key, value) -> put(key, value) }
        event.longParameters.forEach { (key, value) -> put(key, value) }
        event.doubleParameters.forEach { (key, value) -> put(key, value) }
        put("\$exception_level", "error")
        put("\$exception_source", "manual")
        put("\$exception_list", buildJsonArray {
            add(buildJsonObject {
                put("type", exceptionType)
                // 오류 원문은 URL, 인증 헤더와 사용자 입력을 포함할 수 있다.
                put("value", "${operation}_$category")
                put("mechanism", buildJsonObject { put("handled", true); put("synthetic", false) })
                put("stacktrace", buildJsonObject {
                    put("type", "raw")
                    put("frames", buildJsonArray {
                        frames.asReversed().forEach { frame -> add(buildJsonObject {
                            put("platform", "custom"); put("lang", "kotlin")
                            put("function", frame.function)
                            frame.filename?.let { put("filename", it) }
                            frame.line?.let { put("lineno", it) }
                            put("in_app", frame.function.contains("com.chaekchaek") || frame.function.contains("com.chamsae"))
                        }) }
                })
            })
        })
    }.toString()
}

internal data class DiagnosticFrame(val function: String, val filename: String? = null, val line: Int? = null)
internal expect fun diagnosticFrames(error: Throwable): List<DiagnosticFrame>
internal fun diagnosticSymbol(value: String): String = value.take(256).filter {
    it.isLetterOrDigit() && it.code < 128 || it in "._$#:+<>()/-[]"
}.ifEmpty { "unknown" }
