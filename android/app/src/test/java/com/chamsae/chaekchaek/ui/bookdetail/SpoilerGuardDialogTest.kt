package com.chamsae.chaekchaek.ui.bookdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoilerGuardDialogTest {
  @Test
  fun `progress page accepts only values inside the book`() {
    assertEquals(0, validProgressPage("0", 425))
    assertEquals(425, validProgressPage("425", 425))
    assertNull(validProgressPage("426", 425))
    assertNull(validProgressPage("-1", 425))
    assertNull(validProgressPage("쪽", 425))
  }

  @Test
  fun `reflection is locked only when its page is beyond current progress`() {
    assertFalse(shouldLockReflection(currentPage = 80, reflectionPage = 80))
    assertTrue(shouldLockReflection(currentPage = 80, reflectionPage = 160))
    assertFalse(shouldLockReflection(currentPage = 80, reflectionPage = null))
  }

  @Test
  fun `previewed reflection is unlocked without unlocking another reflection on the same page`() {
    assertFalse(shouldLockReflection(currentPage = 80, reflectionPage = 160, previewed = true))
    assertTrue(shouldLockReflection(currentPage = 80, reflectionPage = 160, previewed = false))
  }
}
