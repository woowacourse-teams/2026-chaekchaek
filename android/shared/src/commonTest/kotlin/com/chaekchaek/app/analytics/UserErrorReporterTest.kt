package com.chaekchaek.app.analytics

import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*
import kotlin.time.TestTimeSource
import kotlin.time.Duration.Companion.seconds

class UserErrorReporterTest {
    @Test fun loginAndGeneralHttpFailuresKeepTheSameCategoryAndStatus() = runTest {
        val reports = mutableListOf<UserErrorDetails>()
        val reporter = UserErrorReporter(sink = reports::add)
        for ((status, category) in listOf(
            400 to "validation", 401 to "auth", 403 to "permission", 404 to "not_found",
            409 to "conflict", 422 to "validation", 500 to "server", 503 to "server",
        )) {
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.fromValue(status)) })
            try {
                val response = client.get("https://example.invalid/failure")
                reporter.report(ResponseException(response, ""), "Search")
                reporter.report(MobileLoginException("PRIVATE_SERVER_DETAIL", status), "SignIn")
                val pair = reports.takeLast(2)
                assertEquals(listOf(category, category), pair.map { it.category }, "HTTP $status")
                assertEquals(listOf(status, status), pair.map { it.status }, "HTTP $status")
            } finally {
                client.close()
            }
        }
    }

    @Test fun cancellationIsExcludedAndSameFailureIsReportedOnlyOncePerOperation() {
        val reports = mutableListOf<UserErrorDetails>()
        val reporter = UserErrorReporter(sink = reports::add)
        val error = IllegalStateException("private input")
        reporter.report(CancellationException(), "Search")
        reporter.report(error, "Search")
        reporter.report(error, "Search")
        reporter.report(error, "LibrarySave")
        reporter.report(IllegalStateException("another attempt"), "Search")
        assertEquals(listOf("Search", "LibrarySave", "Search"), reports.map { it.operation })
    }

    @Test fun burstProtectionResetsAfterOneMinuteAndSinkFailureDoesNotBreakRecovery() {
        val time = TestTimeSource()
        var count = 0
        val reporter = UserErrorReporter(sink = { count++ }, timeSource = time)
        repeat(30) { reporter.report(IllegalStateException(), "Search") }
        assertEquals(20, count)
        time += 60.seconds
        reporter.report(IllegalStateException(), "Search")
        assertEquals(21, count)
        val state = ErrorRecoveryState(UserErrorReporter(sink = { error("collector unavailable") }))
        var retried = false
        state.report(IllegalStateException(), ErrorOperation.LibrarySave) { retried = true }
        val notice = assertNotNull(state.notice.value)
        assertFalse(retried)
        state.takeRecovery(notice.id)?.invoke(null)
        assertTrue(retried)
    }

    @Test fun handledErrorsUsePostHogSinkAndKeepScreenAndActionContextWithoutRawMessage() {
        val analytics = mutableListOf<AnalyticsEvent>()
        val errors = mutableListOf<String>()
        val tracker = AnalyticsTracker("test", analytics::add, AnalyticsCollectionController(), errorSink = errors::add)
        tracker.screenView("book_detail")
        val action = tracker.startAction("library_add")
        tracker.errors.report(IllegalStateException("private@example.com Bearer PRIVATE_TOKEN https://api/?query=secret"), "LibrarySave")
        val properties = Json.parseToJsonElement(errors.single()).jsonObject
        assertEquals("book_detail", properties["screen_name"]?.jsonPrimitive?.content)
        assertEquals(action.id, properties["action_id"]?.jsonPrimitive?.content)
        assertEquals("library_add", properties["last_action"]?.jsonPrimitive?.content)
        assertFalse(errors.single().contains("PRIVATE_TOKEN"))
        assertFalse(errors.single().contains("private@example"))
        assertFalse(analytics.any { it.name == "cc_user_error" || it.name == "\$exception" })
        assertEquals("IllegalStateException", properties["\$exception_list"]!!.jsonArray.single().jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test fun remoteEventRuleStopsAndResumesHandledErrorCapture() {
        val errors = mutableListOf<String>()
        val collection = AnalyticsCollectionController()
        val tracker = AnalyticsTracker("test", {}, collection, errorSink = errors::add)
        assertTrue(collection.receive("""{"schema_version":1,"revision":"paused","enabled":true,"sample_rate":1,"events":{"cc_user_error":{"enabled":false}}}"""))
        tracker.errors.report(IllegalStateException(), "Search")
        assertTrue(errors.isEmpty())
        assertTrue(collection.receive("""{"schema_version":1,"revision":"resumed","enabled":true,"sample_rate":1}"""))
        tracker.errors.report(IllegalStateException(), "Search")
        assertEquals("resumed", Json.parseToJsonElement(errors.single()).jsonObject["collection_revision"]?.jsonPrimitive?.content)
    }

    @Test fun privacyFilterRemovesNestedMessagesInputsUrlsAndLocalPathsButKeepsFrames() {
        val raw = buildJsonObject {
            put("screen_name", "search")
            put("email", "private@example.com")
            put("request", "Bearer PRIVATE_TOKEN")
            put("\$exception_message", "PRIVATE_TOKEN")
            put("\$exception_steps", "PRIVATE_TOKEN")
            put("\$exception_list", buildJsonArray { add(buildJsonObject {
                put("type", "IllegalStateException")
                put("value", "PRIVATE_TOKEN")
                put("stacktrace", buildJsonObject { put("type", "raw"); put("frames", buildJsonArray {
                    add(buildJsonObject {
                        put("function", "com.chaekchaek.Save.invoke")
                        put("filename", "/Users/private-owner/Project/Save.kt")
                        put("lineno", 42)
                        put("vars", "PRIVATE_TOKEN")
                        put("context_line", "PRIVATE_TOKEN")
                    })
                }) })
            }) })
            put("\$debug_images", buildJsonArray { add(buildJsonObject {
                put("code_file", "/Users/private-owner/App/Shared")
                put("debug_id", "1234-ABCD")
                put("image_addr", "0x1234")
            }) })
        }.toString()
        val clean = assertNotNull(sanitizeExceptionProperties(raw))
        assertFalse(clean.contains("PRIVATE_TOKEN"))
        assertFalse(clean.contains("private-owner"))
        assertFalse(clean.contains("private@example"))
        val frame = Json.parseToJsonElement(clean).jsonObject["\$exception_list"]!!.jsonArray.single().jsonObject["stacktrace"]!!.jsonObject["frames"]!!.jsonArray.single().jsonObject
        assertEquals("Save.kt", frame["filename"]?.jsonPrimitive?.content)
        assertEquals(42, frame["lineno"]?.jsonPrimitive?.int)
        assertEquals("com.chaekchaek.Save.invoke", frame["function"]?.jsonPrimitive?.content)
        assertNull(sanitizeExceptionProperties("broken"))
    }
}
