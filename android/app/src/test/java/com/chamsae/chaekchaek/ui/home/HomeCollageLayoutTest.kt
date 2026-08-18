package com.chamsae.chaekchaek.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCollageLayoutTest {
    @Test
    fun layout_isStableForSameRanking_andSafeForTitleArea() {
        val ranking = listOf("book-1", "book-2", "book-3", "book-4", "book-5", "book-6")
        val layout = collagePlacements(ranking)

        assertEquals(layout, collagePlacements(ranking))
        assertNotEquals(layout, collagePlacements(ranking.reversed()))
        assertTrue(layout.zipWithNext().all { (higher, lower) -> higher.width > lower.width })
        assertTrue(layout.all { it.x >= 0 && it.x + it.width <= 390 && it.y + it.height <= 181 })
    }

    @Test
    fun swipe_movesBooksThroughScatteredSlots() {
        assertEquals(0, collageSlotIndex(bookIndex = 0, selectedIndex = 0, bookCount = 6))
        assertEquals(1, collageSlotIndex(bookIndex = 1, selectedIndex = 0, bookCount = 6))
        assertEquals(2, collageSlotIndex(bookIndex = 0, selectedIndex = 1, bookCount = 6))
        assertEquals(0, collageSlotIndex(bookIndex = 1, selectedIndex = 1, bookCount = 6))
    }

    @Test
    fun swipe_changesSelectionAfterThreshold_andWraps() {
        assertEquals(0, collageSelectionAfterSwipe(0, 6, dragDistance = -47f, threshold = 48f))
        assertEquals(1, collageSelectionAfterSwipe(0, 6, dragDistance = -48f, threshold = 48f))
        assertEquals(5, collageSelectionAfterSwipe(0, 6, dragDistance = 48f, threshold = 48f))
    }

    @Test
    fun freePlacement_staysInsideCollageBounds() {
        val topLeft = constrainedCollagePosition(-20f, -10f, 118f, 177f, 390f, 190f)
        val bottomRight = constrainedCollagePosition(400f, 200f, 118f, 177f, 390f, 190f)

        assertEquals(0f, topLeft.x)
        assertEquals(0f, topLeft.y)
        assertEquals(272f, bottomRight.x)
        assertEquals(13f, bottomRight.y)
    }

    @Test
    fun readingProgress_isClampedAndHandlesMissingTotal() {
        assertEquals(0.4125f, readingProgress(currentPage = 132, totalPages = 320))
        assertEquals(0f, readingProgress(currentPage = 10, totalPages = 0))
        assertEquals(1f, readingProgress(currentPage = 400, totalPages = 320))
    }
}
