package com.chaekchaek.app.ui.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFieldValueTest {
    @Test
    fun longerExternalQueryPlacesCursorAtTheEnd() {
        val current = TextFieldState("책", TextRange(1))
        val externalQuery = "긴 최근 검색어"

        current.syncExternalQuery(externalQuery)

        assertEquals(externalQuery, current.text.toString())
        assertEquals(TextRange(externalQuery.length), current.selection)
    }

    @Test
    fun unchangedExternalQueryPreservesSelectionAndComposition() {
        val current = TextFieldState("검색 입력", TextRange(2))

        current.syncExternalQuery(current.text.toString())

        assertEquals(TextRange(2), current.selection)
    }
}
