package com.chamsae.chaekchaek

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathIterator
import androidx.compose.ui.graphics.PathSegment
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GraphicsPathNativeTest {
    @Test
    fun pathIteratorLoadsNativeLibrary() {
        val path = Path().apply { moveTo(1f, 2f) }

        assertEquals(PathSegment.Type.Move, PathIterator(path).next().type)
    }
}
