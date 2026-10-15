package com.chaekchaek.app.ui.onboarding

import com.chaekchaek.app.domain.book.BookSearchPage
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
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

            viewModel.save("token")
            advanceUntilIdle()

            assertEquals(listOf("1", "2"), saved)
            assertTrue(viewModel.state.value.saveCompleted)
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
            viewModel.save("token")
            advanceUntilIdle()

            assertEquals(OnboardingStep.SAVE_ERROR, viewModel.state.value.step)
            assertEquals(listOf(selected), viewModel.state.value.selectedBooks)

            viewModel.retrySave("token")
            advanceUntilIdle()
            assertEquals(2, attempts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryDoesNotSaveBooksThatAlreadySucceededAgain() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val first = book("1", "첫 책")
        val second = book("2", "둘째 책")
        val attempts = mutableListOf<String>()
        var secondAttempts = 0
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            addBook = { book, _ ->
                attempts += book.isbn13
                if (book == second && secondAttempts++ == 0) error("offline")
            },
        )
        try {
            viewModel.startSearch()
            viewModel.toggle(first)
            viewModel.toggle(second)
            viewModel.reviewSelection()
            viewModel.save("token")
            advanceUntilIdle()

            viewModel.retrySave("token")
            advanceUntilIdle()

            assertEquals(listOf("1", "2", "2"), attempts)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loginFailureKeepsSelectionAndOffersLoginAgain() {
        val selected = book("1", "첫 책")
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
        )

        viewModel.startSearch()
        viewModel.toggle(selected)
        viewModel.reviewSelection()
        viewModel.showLoginError("로그인 실패")

        assertEquals(OnboardingStep.LOGIN_ERROR, viewModel.state.value.step)
        assertEquals(listOf(selected), viewModel.state.value.selectedBooks)
    }

    @Test
    fun skipCancelsInFlightSaveWithoutCompletingOnboarding() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val selected = book("1", "첫 책")
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            addBook = { _, _ -> awaitCancellation() },
        )
        try {
            viewModel.startSearch()
            viewModel.toggle(selected)
            viewModel.reviewSelection()
            viewModel.save("token")
            runCurrent()

            viewModel.cancelPendingWork()
            advanceUntilIdle()

            assertFalse(viewModel.state.value.saveCompleted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun queryLimitPreservesEmojiAndExistingSuffixOnPaste() {
        val viewModel = FirstLibraryOnboardingViewModel()
        viewModel.updateQuery("가".repeat(99) + "😀추가")
        assertEquals("가".repeat(99) + "😀", viewModel.state.value.query)
        viewModel.updateQuery("")
        val original = "앞".repeat(50) + "뒤".repeat(50)
        viewModel.updateQuery(original)
        viewModel.updateQuery("앞".repeat(50) + "붙여넣기" + "뒤".repeat(50))
        assertEquals(original, viewModel.state.value.query)
    }

    @Test
    fun repeatedAndStaleBackRequestsCannotCloseAnotherStep() {
        val viewModel = FirstLibraryOnboardingViewModel()
        viewModel.startSearch()
        viewModel.toggle(book("1", "첫 책"))
        viewModel.reviewSelection()
        val choice = viewModel.state.value
        viewModel.returnToPreviousStep(choice.step, choice.stepRevision)
        viewModel.returnToPreviousStep(choice.step, choice.stepRevision)
        assertEquals(OnboardingStep.SEARCH, viewModel.state.value.step)
        viewModel.reviewSelection()
        viewModel.returnToPreviousStep(choice.step, choice.stepRevision)
        assertEquals(OnboardingStep.CHOICE, viewModel.state.value.step)
    }

    @Test
    fun searchAndSaveIndicatorsWaitFor500MillisAndCloseOnResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val searchResponse = CompletableDeferred<BookSearchPage>()
        val saveResponse = CompletableDeferred<Unit>()
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> searchResponse.await() },
            addBook = { _, _ -> saveResponse.await() },
        )
        try {
            viewModel.startSearch()
            viewModel.updateQuery("책")
            viewModel.search()
            runCurrent()
            advanceTimeBy(499)
            assertFalse(viewModel.state.value.showSearchLoading)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(viewModel.state.value.showSearchLoading)
            searchResponse.complete(BookSearchPage(0, null, emptyList()))
            runCurrent()
            assertFalse(viewModel.state.value.showSearchLoading)

            viewModel.toggle(book("1", "첫 책"))
            viewModel.reviewSelection()
            viewModel.save("token")
            runCurrent()
            advanceTimeBy(499)
            assertFalse(viewModel.state.value.showSaveLoading)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(viewModel.state.value.showSaveLoading)
            saveResponse.complete(Unit)
            runCurrent()
            assertFalse(viewModel.state.value.showSaveLoading)
        } finally {
            viewModel.cancelPendingWork()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun leavingOnboardingCancelsRemainingBooksAndCompletion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val firstResponse = CompletableDeferred<Unit>()
        val attempts = mutableListOf<String>()
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            addBook = { book, _ -> attempts += book.isbn13; firstResponse.await() },
        )
        try {
            viewModel.toggle(book("1", "첫 책"))
            viewModel.toggle(book("2", "둘째 책"))
            viewModel.save("token")
            runCurrent()
            viewModel.cancelPendingWork()
            firstResponse.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf("1"), attempts)
            assertFalse(viewModel.state.value.saveCompleted)
            assertFalse(viewModel.state.value.showSaveLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retryUsesTheCurrentTokenAfterSessionRenewal() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val tokens = mutableListOf<String>()
        val viewModel = FirstLibraryOnboardingViewModel(
            searchRepository = BookSearchRepository { _, _, _ -> BookSearchPage(0, null, emptyList()) },
            addBook = { _, token ->
                tokens += token
                if (token == "expired-token") error("expired")
            },
        )
        try {
            viewModel.toggle(book("1", "첫 책"))
            viewModel.save("expired-token")
            advanceUntilIdle()
            viewModel.retrySave("renewed-token")
            advanceUntilIdle()
            assertEquals(listOf("expired-token", "renewed-token"), tokens)
            assertTrue(viewModel.state.value.saveCompleted)
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
