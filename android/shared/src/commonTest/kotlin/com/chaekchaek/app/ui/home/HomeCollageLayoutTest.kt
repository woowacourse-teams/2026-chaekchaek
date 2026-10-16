package com.chaekchaek.app.ui.home

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeCollageLayoutTest {
    @Test
    fun layoutIsStableAndFitsTheHero() {
        val ranking = listOf("book-1", "book-2", "book-3", "book-4", "book-5", "book-6")
        val layout = collagePlacements(ranking)

        assertEquals(layout, collagePlacements(ranking))
        assertEquals(
            listOf(
                CollagePlacement(141, 29, 108, 155, 0f),
                CollagePlacement(225, 53, 79, 113, 13f),
                CollagePlacement(63, 48, 79, 113, -15f),
                CollagePlacement(271, 84, 65, 93, 24f),
                CollagePlacement(23, 80, 65, 93, -23f),
                CollagePlacement(172, 4, 80, 114, 10f),
            ),
            layout,
        )
        assertTrue(layout.all { it.x >= 0 && it.x + it.width <= 390 && it.y + it.height <= 218 })
    }

    @Test
    fun swipeUsesThresholdAndWraps() {
        assertEquals(0, collageSelectionAfterSwipe(0, 6, dragDistance = -47f, threshold = 48f))
        assertEquals(1, collageSelectionAfterSwipe(0, 6, dragDistance = -48f, threshold = 48f))
        assertEquals(5, collageSelectionAfterSwipe(0, 6, dragDistance = 48f, threshold = 48f))
    }

    @Test
    fun selectedCoverTransformKeepsTheApprovedCenterPlacement() {
        val transform = collageTransform(
            CollagePlacement(141, 29, 108, 155, 0f),
            canvasScale = 1f,
        )

        assertEquals(CollageTransform(141f, 29f, 1f, 1f, 0f), transform)
    }

    @Test
    fun readingProgressIsClamped() {
        assertEquals(0.4125f, readingProgress(currentPage = 132, totalPages = 320))
        assertEquals(0f, readingProgress(currentPage = 10, totalPages = 0))
        assertEquals(1f, readingProgress(currentPage = 400, totalPages = 320))
    }

    @Test
    fun recentReflectionCardFitsTheAvailableWidthWithoutGrowingPastTheFigmaWidth() {
        assertEquals(272.dp, recentReflectionCardWidth(272.dp))
        assertEquals(314.dp, recentReflectionCardWidth(342.dp))
    }
}
