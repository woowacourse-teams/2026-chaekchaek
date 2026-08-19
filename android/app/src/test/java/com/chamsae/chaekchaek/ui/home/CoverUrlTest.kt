package com.chamsae.chaekchaek.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverUrlTest {
    @Test
    fun `HTTPS 표지만 원격 이미지로 판별한다`() {
        assertTrue("https://example.com/cover.jpg".isRemoteCoverUrl())
        assertFalse("cover-01".isRemoteCoverUrl())
        assertFalse("http://example.com/cover.jpg".isRemoteCoverUrl())
    }
}
