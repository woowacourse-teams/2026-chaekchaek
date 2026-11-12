package com.chaekchaek.app.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 외부 값에서 화면에 필요한 ISBN만 추출한다. 쿼리·인증정보·인코딩된 경로는 허용하지 않는다. */
object BookDeepLink {
    const val maximumUriLength = 128
    const val exampleUri = "chaekchaek://books/9788937460449"
    const val invalidExampleUri = "chaekchaek://books/not-a-book"
    private val pattern = Regex("chaekchaek://books/((?:978|979)[0-9]{10})", RegexOption.IGNORE_CASE)

    fun isbn13(uri: String): String? {
        if (uri.length > maximumUriLength || uri.any { it.code > 0x7f }) return null
        val isbn = pattern.matchEntire(uri)?.groupValues?.get(1) ?: return null
        return isbn.takeIf { value ->
            value.withIndex().sumOf { (index, digit) -> (digit - '0') * if (index % 2 == 0) 1 else 3 } % 10 == 0
        }
    }
}

data class BookDeepLinkDelivery(val id: String, val isbn13: String?) {
    /** 원본 URI는 저장하지 않는다. 복원 시에도 같은 검증 경로를 통과한다. */
    val restorableUri: String get() = isbn13?.let { "chaekchaek://books/$it" } ?: BookDeepLink.invalidExampleUri
}

/** 플랫폼 초기화·온보딩보다 먼저 도착한 링크를 보관한다. 새 링크는 이전 대기 링크를 대체한다. */
class BookDeepLinkInbox {
    private val state = MutableStateFlow<BookDeepLinkDelivery?>(null)
    val pending = state.asStateFlow()

    fun submit(uri: String) = receive(uri, newDeliveryId())

    fun receive(uri: String, deliveryId: String) {
        val id = deliveryId.takeIf { it.length == 36 && it.all { c -> c in '0'..'9' || c in 'a'..'f' || c == '-' } }
            ?: newDeliveryId()
        state.value = BookDeepLinkDelivery(id, BookDeepLink.isbn13(uri))
    }

    fun acknowledge(deliveryId: String) {
        if (state.value?.id == deliveryId) state.value = null
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun newDeliveryId(): String = Uuid.random().toString()
