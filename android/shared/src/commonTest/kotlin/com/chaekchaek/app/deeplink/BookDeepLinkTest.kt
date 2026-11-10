package com.chaekchaek.app.deeplink

import kotlin.test.*

class BookDeepLinkTest {
    @Test fun validIsbnAndCaseInsensitiveScheme() {
        assertEquals("9788937460449", BookDeepLink.isbn13(BookDeepLink.exampleUri))
        assertEquals("9788937460449", BookDeepLink.isbn13("CHAEKCHAEK://BOOKS/9788937460449"))
        assertEquals("9791172376215", BookDeepLink.isbn13("chaekchaek://books/9791172376215"))
    }

    @Test fun externalInputMustMatchTheWholeCanonicalRoute() {
        listOf(
            "https://books/9788937460449", "chaekchaek://other/9788937460449",
            "chaekchaek://books/9788937460448", "chaekchaek://books/1234567890128",
            "chaekchaek://user@books/9788937460449", "chaekchaek://books:80/9788937460449",
            "chaekchaek://books/%39%3788937460449", "chaekchaek://books/9788937460449/",
            "chaekchaek://books/9788937460449?token=untrusted", "chaekchaek://books/9788937460449#section",
            " ${BookDeepLink.exampleUri}", "${BookDeepLink.exampleUri}\n", "a".repeat(1_000_000), "",
        ).forEach { assertNull(BookDeepLink.isbn13(it), it.take(128)) }
    }

    @Test fun newestPendingLinkWinsAndOldAcknowledgementCannotRemoveIt() {
        val inbox = BookDeepLinkInbox()
        inbox.submit(BookDeepLink.exampleUri)
        val first = assertNotNull(inbox.pending.value)
        inbox.submit(BookDeepLink.exampleUri)
        val second = assertNotNull(inbox.pending.value)
        assertNotEquals(first.id, second.id)
        inbox.acknowledge(first.id)
        assertEquals(second, inbox.pending.value)
        inbox.acknowledge(second.id)
        assertNull(inbox.pending.value)
    }

    @Test fun invalidInputIsNotRetainedInRestorableState() {
        val inbox = BookDeepLinkInbox()
        inbox.receive("chaekchaek://books/not-a-book?private=example", "bad-id")
        val pending = assertNotNull(inbox.pending.value)
        assertNull(pending.isbn13)
        assertEquals(BookDeepLink.invalidExampleUri, pending.restorableUri)
        assertEquals(36, pending.id.length)
    }
}
