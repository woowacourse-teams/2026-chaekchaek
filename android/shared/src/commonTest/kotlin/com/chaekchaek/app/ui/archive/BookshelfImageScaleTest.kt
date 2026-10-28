package com.chaekchaek.app.ui.archive

import kotlin.test.Test
import kotlin.test.assertEquals

class BookshelfImageScaleTest {
    @Test fun `배율은 100퍼센트부터 200퍼센트 사이에서 5퍼센트 단위로 선택한다`() {
        assertEquals(100, snapBookshelfImageScale(90f))
        assertEquals(100, snapBookshelfImageScale(102f))
        assertEquals(105, snapBookshelfImageScale(103f))
        assertEquals(140, snapBookshelfImageScale(140f))
        assertEquals(200, snapBookshelfImageScale(198f))
        assertEquals(200, snapBookshelfImageScale(240f))
    }
}
