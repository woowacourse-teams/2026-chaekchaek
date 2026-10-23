package com.chaekchaek.app.ui.publiclibrary

import com.chaekchaek.app.presentation.common.ErrorAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PublicLibraryViewModelTest {
    @Test fun delayedLoadingAndConcurrentRetryAreBoundToOneRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val result = CompletableDeferred<String>()
            var reads = 0
            val vm = PublicLibraryViewModel { reads++; result.await() }
            vm.retry(); runCurrent()
            assertEquals(1, reads)
            assertFalse(vm.uiState.value.showLoading)
            advanceTimeBy(500); runCurrent()
            assertTrue(vm.uiState.value.showLoading)
            result.complete("서재"); advanceUntilIdle()
            assertEquals("서재", vm.uiState.value.content)
            assertFalse(vm.uiState.value.showLoading)
            assertFalse(vm.uiState.value.loading)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun failedLoadWaitsForExplicitRetryAndKeepsExistingContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var reads = 0
            val vm = PublicLibraryViewModel {
                reads++
                if (reads == 2) error("offline")
                "서재 $reads"
            }
            advanceUntilIdle()
            vm.retry(); advanceUntilIdle()
            assertEquals(2, reads)
            assertEquals("서재 1", vm.uiState.value.content)
            assertEquals(ErrorAction.Retry, vm.errors.notice.value?.action)
            val recovery = vm.errors.takeRecovery(vm.errors.notice.value!!.id)
            recovery!!(null); advanceUntilIdle()
            assertEquals("서재 3", vm.uiState.value.content)
            assertNull(vm.errors.notice.value)
        } finally { Dispatchers.resetMain() }
    }
}
