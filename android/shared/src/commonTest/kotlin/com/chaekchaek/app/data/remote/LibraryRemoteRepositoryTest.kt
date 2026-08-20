package com.chaekchaek.app.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRemoteRepositoryTest {

  @Test
  fun `bulk book ids are unique and limited to ten per request`() {
    val chunks = chunkLibraryBookIds((1L..11L).toList() + 1L)

    assertEquals(listOf((1L..10L).toList(), listOf(11L)), chunks)
  }
}
