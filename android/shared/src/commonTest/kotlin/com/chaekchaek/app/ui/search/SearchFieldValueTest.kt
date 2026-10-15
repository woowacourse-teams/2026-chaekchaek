package com.chaekchaek.app.ui.search

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SearchFieldValueTest {
    @Test
    fun longerExternalQueryPlacesCursorAtTheEnd() {
        val current = TextFieldValue(
            text = "책",
            selection = TextRange(1),
        )
        val externalQuery = "긴 최근 검색어"

        val updated = current.syncExternalQuery(externalQuery)

        assertEquals(externalQuery, updated.text)
        assertEquals(TextRange(externalQuery.length), updated.selection)
        assertEquals(null, updated.composition)
    }

    @Test
    fun unchangedExternalQueryPreservesSelectionAndComposition() {
        val current = TextFieldValue(
            text = "검색 입력",
            selection = TextRange(2),
            composition = TextRange(1, 3),
        )

        val unchanged = current.syncExternalQuery(current.text)

        assertSame(current, unchanged)
        assertEquals(TextRange(2), unchanged.selection)
        assertEquals(TextRange(1, 3), unchanged.composition)
    }
}
