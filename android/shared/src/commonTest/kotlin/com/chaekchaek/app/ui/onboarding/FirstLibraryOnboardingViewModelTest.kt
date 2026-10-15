package com.chaekchaek.app.ui.onboarding

import com.chaekchaek.app.domain.book.BookSearchPage
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirstLibraryOnboardingViewModelTest {
    @Test
    fun installStorageShowsOnlyBeforeFirstExposure() {
        var shown = false
        val storage = OnboardingStorage(hasBeenShown = { shown }, markShown = { shown = true })

        assertTrue(shouldShowFirstLibraryOnboarding(storage))
        storage.markShown()
        assertFalse(shouldShowFirstLibraryOnboarding(storage))
    }

    @Test
    fun selectionIsKeptUntilLoginAndSavedInOrder() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val first = book("1", "첫 책")
        val second = book("2", "둘째 책")
        val saved = mutableListOf<String>()
        var completed = false
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(2, null, listOf(first, second)) },
            addBook = { book, _ -> saved += book.isbn13 },
        )
        try {
            viewModel.startSearch()
            viewModel.updateQuery("책")
            viewModel.search()
            advanceUntilIdle()
            viewModel.toggle(first)
            viewModel.toggle(second)
            viewModel.reviewSelection()

            assertEquals(OnboardingStep.CHOICE, viewModel.state.value.step)
            assertEquals(listOf(first, second), viewModel.state.value.selectedBooks)

            viewModel.save("token") { completed = true }
            advanceUntilIdle()

            assertEquals(listOf("1", "2"), saved)
            assertTrue(completed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedSaveKeepsSelectionForRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val selected = book("1", "첫 책")
        var attempts = 0
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            addBook = { _, _ ->
                attempts += 1
                if (attempts == 1) error("offline")
            },
        )
        try {
            viewModel.startSearch()
            viewModel.toggle(selected)
            viewModel.reviewSelection()
            viewModel.save("token") {}
            advanceUntilIdle()

            assertEquals(OnboardingStep.SAVE_ERROR, viewModel.state.value.step)
            assertEquals(listOf(selected), viewModel.state.value.selectedBooks)

            viewModel.retrySave {}
            advanceUntilIdle()
            assertEquals(2, attempts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun book(isbn: String, title: String) = BookSearchResult(
        title = title,
        creator = "작가",
        publisher = "출판사",
        year = "2026",
        coverUrl = "",
        isbn13 = isbn,
        totalPages = 100,
    )
}
