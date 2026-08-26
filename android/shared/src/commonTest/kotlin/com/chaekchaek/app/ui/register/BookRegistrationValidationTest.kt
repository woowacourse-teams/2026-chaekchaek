package com.chaekchaek.app.ui.register

import com.chaekchaek.app.domain.book.BookSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookRegistrationValidationTest {
    @Test
    fun requiresServerRegistrationFields() {
        val valid = BookSearchResult(
            title = "마션",
            creator = "앤디 위어",
            publisher = "알에이치코리아",
            year = "2026",
            coverUrl = "",
            isbn13 = "9780000000000",
            totalPages = 308,
        )

        assertNull(valid.registrationValidationError())
        assertEquals("ISBN 정보가 없는 책은 등록할 수 없어요", valid.copy(isbn13 = "").registrationValidationError())
        assertEquals("책 제목을 입력해 주세요", valid.copy(title = " ").registrationValidationError())
        assertEquals("저자를 입력해 주세요", valid.copy(creator = " ").registrationValidationError())
        assertEquals("전체 쪽수는 0보다 작을 수 없어요", valid.copy(totalPages = -1).registrationValidationError())
    }
}
