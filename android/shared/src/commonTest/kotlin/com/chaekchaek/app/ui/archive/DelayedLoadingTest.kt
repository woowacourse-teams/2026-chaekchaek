package com.chaekchaek.app.ui.archive

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DelayedLoadingTest {
    @Test
    fun indicatorAppearsOnlyAfter500MillisecondsAndClosesWithResponse() = runTest {
        val changes = mutableListOf<Boolean>()
        val request = launch {
            withDelayedLoading(changes::add) {
                delay(700)
                "done"
            }
        }

        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        assertEquals(emptyList(), changes)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(true), changes)

        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf(true, false), changes)
        request.join()
    }

    @Test
    fun fastRequestNeverShowsIndicator() = runTest {
        val changes = mutableListOf<Boolean>()

        withDelayedLoading(changes::add) { "done" }

        assertEquals(listOf(false), changes)
    }
}
