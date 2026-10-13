package com.chaekchaek.app.ui.register

import kotlin.test.Test
import kotlin.test.assertEquals

class BookRegisterInputRulesTest {
    @Test
    fun textFieldsAcceptOnlyTheirMaximumLengths() {
        assertEquals(
            "제".repeat(BookRegisterInputRules.MAX_TITLE_LENGTH),
            BookRegisterInputRules.limitTitle("제".repeat(BookRegisterInputRules.MAX_TITLE_LENGTH + 1)),
        )
        assertEquals(
            "저".repeat(BookRegisterInputRules.MAX_CREATOR_LENGTH),
            BookRegisterInputRules.limitCreator("저".repeat(BookRegisterInputRules.MAX_CREATOR_LENGTH + 1)),
        )
        assertEquals(
            "출".repeat(BookRegisterInputRules.MAX_PUBLISHER_LENGTH),
            BookRegisterInputRules.limitPublisher("출".repeat(BookRegisterInputRules.MAX_PUBLISHER_LENGTH + 1)),
        )
        assertEquals(
            "평".repeat(BookRegisterInputRules.MAX_NOTE_LENGTH),
            BookRegisterInputRules.limitNote("평".repeat(BookRegisterInputRules.MAX_NOTE_LENGTH + 1)),
        )
    }

    @Test
    fun yearAcceptsOnlyFourDigits() {
        assertEquals("2026", BookRegisterInputRules.limitYear("20년26월123"))
    }
}
