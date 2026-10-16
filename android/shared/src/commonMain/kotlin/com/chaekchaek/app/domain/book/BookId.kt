package com.chaekchaek.app.domain.book

import kotlin.jvm.JvmInline

/**
 * 식별자는 규칙이 없어 검증하지 않는다. 목적은 자리 바꿈 방지다.
 *
 * API 경로가 `/books/{bookId}/notes` 와 `/notes/{noteId}/replies` 처럼 중첩되어 두 식별자를
 * 같은 함수에서 다루는 곳이 많다. 둘 다 String 이면 자리를 바꿔 넣어도 컴파일된다.
 */
@JvmInline
value class BookId(val value: String)
