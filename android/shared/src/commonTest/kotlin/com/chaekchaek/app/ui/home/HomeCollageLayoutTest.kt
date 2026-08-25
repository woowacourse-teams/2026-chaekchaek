package com.chaekchaek.app.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HomeCollageLayoutTest {
    @Test
    fun layoutIsStableAndFitsTheHero() {
        val ranking = listOf("book-1", "book-2", "book-3", "book-4", "book-5", "book-6")
        val layout = collagePlacements(ranking)

        assertEquals(layout, collagePlacements(ranking))
        assertNotEquals(layout, collagePlacements(ranking.reversed()))
        assertTrue(layout.zipWithNext().all { (higher, lower) -> higher.width > lower.width })
        assertTrue(layout.all { it.x >= 0 && it.x + it.width <= 390 && it.y + it.height <= 181 })
    }

    @Test
    fun swipeUsesThresholdAndWraps() {
        assertEquals(0, collageSelectionAfterSwipe(0, 6, dragDistance = -47f, threshold = 48f))
        assertEquals(1, collageSelectionAfterSwipe(0, 6, dragDistance = -48f, threshold = 48f))
        assertEquals(5, collageSelectionAfterSwipe(0, 6, dragDistance = 48f, threshold = 48f))
    }

    @Test
    fun readingProgressIsClamped() {
        assertEquals(0.4125f, readingProgress(currentPage = 132, totalPages = 320))
        assertEquals(0f, readingProgress(currentPage = 10, totalPages = 0))
        assertEquals(1f, readingProgress(currentPage = 400, totalPages = 320))
    }
}
