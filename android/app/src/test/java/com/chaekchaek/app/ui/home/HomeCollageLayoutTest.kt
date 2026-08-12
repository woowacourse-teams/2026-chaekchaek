package com.chaekchaek.app.ui.home

import androidx.compose.ui.geometry.Offset
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
    fun draggedBook_staysInsideCollage() {
        val moved = constrainedDragOffset(
            baseX = 120f,
            baseY = 4f,
            width = 118f,
            height = 177f,
            current = Offset.Zero,
            delta = Offset(500f, 500f),
            canvasWidth = 390f,
        )

        assertEquals(152f, moved.x)
        assertEquals(55f, moved.y)
    }

    @Test
    fun readingProgress_isClampedAndHandlesMissingTotal() {
        assertEquals(0.4125f, readingProgress(currentPage = 132, totalPages = 320))
        assertEquals(0f, readingProgress(currentPage = 10, totalPages = 0))
        assertEquals(1f, readingProgress(currentPage = 400, totalPages = 320))
    }
}
