package com.chamsae.chaekchaek.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApiLoadingTest {
  @Test
  fun `500ms 전에 끝난 요청은 로딩을 표시하지 않는다`() = runTest {
    val changes = mutableListOf<Boolean>()

    withDelayedApiLoading(changes::add) { delay(499) }

    assertEquals(listOf(false), changes)
  }

  @Test
  fun `500ms가 지난 요청은 로딩을 표시하고 완료 즉시 닫는다`() = runTest {
    val changes = mutableListOf<Boolean>()
    val request = launch {
      withDelayedApiLoading(changes::add) { delay(750) }
    }

    advanceTimeBy(500)
    runCurrent()
    assertEquals(listOf(true), changes)

    advanceTimeBy(250)
    request.join()
    assertEquals(listOf(true, false), changes)
  }
}
