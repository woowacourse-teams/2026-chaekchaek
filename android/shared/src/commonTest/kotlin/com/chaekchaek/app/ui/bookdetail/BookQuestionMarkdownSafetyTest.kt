package com.chaekchaek.app.ui.bookdetail

import androidx.compose.ui.platform.UriHandler
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookQuestionMarkdownSafetyTest {
    @Test
    fun absoluteWebLinksKeepTheirPathQueryAndFragment() {
        listOf(
            "https://chaekchaek.com/books?title=%EC%B1%85#question",
            "http://example.com:8080/book/42",
            "https://example.com:1/reference",
            "https://example.com:65535/reference",
            "https://127.0.0.1/reference",
            "HTTPS://example.com/reference",
            "https://책췍.com/reference",
        ).forEach { url -> assertEquals(url, allowedBookQuestionMarkdownUrl(url), url) }
    }

    @Test
    fun otherSchemesAndRelativeLinksCannotLeaveTheReader() {
        listOf(
            "javascript:alert(1)", "data:text/html,test", "file:///tmp/book", "intent://book",
            "mailto:reader@example.com", "//example.com", "/reference", "question", "#question",
            "https:example.com", "https:///example.com", "https://", "https://?book=42",
        ).forEach { url -> assertNull(allowedBookQuestionMarkdownUrl(url), url) }
    }

    @Test
    fun ambiguousAuthoritiesAndMalformedPortsCannotLeaveTheReader() {
        listOf(
            " https://example.com", "https://example.com ", "https://exam\nple.com",
            "https://example.com/\tbook", "https://example.com/\u0000book", "https://example.com/\u007F",
            "https://example.com\\@evil.com", "https://reader@example.com", "https://:password@example.com",
            "https://%65xample.com", "https://example.com%2Fevil.com", "https://example.com:wrong",
            "https://example.com:0", "https://example.com:65536", "https://.example.com",
            "https://example.com:", "https://example.com:/reference", "https://example.com:+80",
            "https://example.com:-1", "https://example.com:443:80", "https://example.com:2147483648",
            "https://example..com", "https://-example.com", "https://example-.com", "https://<example>",
            "https://999.999.999.999", "https://127.1", "https://[not-an-ip]",
        ).forEach { url -> assertNull(allowedBookQuestionMarkdownUrl(url), url) }
    }

    @Test
    fun referenceLinksAreCheckedAfterTheirDestinationIsResolved() {
        val opened = mutableListOf<String>()
        val handler = BookQuestionMarkdownUriHandler(object : UriHandler {
            override fun openUri(uri: String) { opened += uri }
        })
        val references = ReferenceLinkHandlerImpl().apply {
            store("safe", "https://example.com/scene")
            store("unsafe", "javascript:alert(1)")
            store("confusing", "https://reader@example.com")
        }

        handler.openUri(references.find("safe"))
        handler.openUri(references.find("unsafe"))
        handler.openUri(references.find("confusing"))
        handler.openUri(references.find("missing"))

        assertEquals(listOf("https://example.com/scene"), opened)
    }

    @Test
    fun platformNavigationFailureDoesNotEscapeTheReader() {
        val handler = BookQuestionMarkdownUriHandler(object : UriHandler {
            override fun openUri(uri: String) { error("지원하는 앱이 없음") }
        })

        handler.openUri("https://example.com/scene")
    }
}
