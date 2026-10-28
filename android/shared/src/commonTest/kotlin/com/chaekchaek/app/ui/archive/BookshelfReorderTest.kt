package com.chaekchaek.app.ui.archive

import kotlin.test.*

class BookshelfReorderTest {
    @Test fun `holding previews a measured destination and saves only on drop`() {
        val drag = BookshelfReorder()
        assertTrue(drag.begin(listOf(1, 2, 3), 1))
        val target = reorderTarget(175f, listOf(ReorderItemBounds(1, 0f, 20f), ReorderItemBounds(2, 20f, 160f), ReorderItemBounds(3, 160f, 185f)))
        assertEquals(3L, target)
        assertTrue(drag.preview(target!!))
        assertEquals(listOf(2L, 3L, 1L), drag.candidate)
        assertEquals(listOf(2L, 3L, 1L), drag.finish())
        assertNull(drag.finish())
    }

    @Test fun `cancelling or returning to the original order never saves`() {
        val drag = BookshelfReorder()
        drag.begin(listOf(1, 2), 1); drag.preview(2)
        assertNull(drag.finish(cancelled = true))
        assertNull(drag.heldId)
        drag.begin(listOf(1, 2), 1); drag.preview(2); drag.preview(2)
        assertNull(drag.finish())
    }

    @Test fun `invalid ids cannot start or change a drag`() {
        val drag = BookshelfReorder()
        assertFalse(drag.begin(listOf(1, 1), 1))
        assertTrue(drag.begin(listOf(1, 2), 1))
        assertFalse(drag.begin(listOf(1, 2), 2))
        assertFalse(drag.preview(9))
        assertNull(drag.finish())
    }

    @Test fun `edge scrolling is bounded and stops in the middle`() {
        assertEquals(-12f, reorderEdgeScroll(-50f, 0f, 300f, 48f, 12f))
        assertEquals(12f, reorderEdgeScroll(350f, 0f, 300f, 48f, 12f))
        assertEquals(0f, reorderEdgeScroll(150f, 0f, 300f, 48f, 12f))
        assertEquals(6f, reorderEdgeScroll(276f, 0f, 300f, 48f, 12f))
    }
}
