package com.chamsae.chaekchaek

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.ui.App
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingScreen
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingState
import com.chaekchaek.app.ui.onboarding.OnboardingStep
import com.chaekchaek.app.ui.onboarding.OnboardingStorage
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstLibraryOnboardingInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun systemBackAndHeaderReturnToThePreviousOnboardingStep() {
        compose.setContent {
            App(
                authPlatform = AuthPlatformCallbacks(
                    requestGoogleIdToken = { _ -> },
                    readRefreshToken = { null },
                    writeRefreshToken = {},
                    clearRefreshToken = {},
                ),
                onboardingStorage = remember { OnboardingStorage(hasBeenShown = { false }, markShown = {}) },
            )
        }
        compose.onNodeWithText("책 찾기").performClick()
        compose.onNodeWithText("어떤 책을 읽고 있나요?").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText("내 서재의 첫 책을\n골라볼까요?").assertIsDisplayed()
        compose.onNodeWithText("책 찾기").performClick()
        compose.onNodeWithContentDescription("이전").performClick()
        compose.onNodeWithText("내 서재의 첫 책을\n골라볼까요?").assertIsDisplayed()
    }

    @Test
    fun bookRowExposesItsCheckboxRoleAndCheckedState() {
        val book = BookSearchResult(title = "첫 책", creator = "작가", publisher = "출판사", year = "2026", coverUrl = "", isbn13 = "1", totalPages = 100)
        compose.setContent {
            var selected by remember { mutableStateOf(false) }
            ChaekchaekTheme {
                FirstLibraryOnboardingScreen(
                    state = FirstLibraryOnboardingState(
                        step = OnboardingStep.SEARCH,
                        results = listOf(book),
                        selectedBooks = if (selected) listOf(book) else emptyList(),
                    ),
                    onStartSearch = {}, onBack = {}, onQueryChange = {}, onSearch = {},
                    onToggleBook = { selected = !selected }, onReviewSelection = {},
                    onLogin = {}, onRetrySave = {}, onSkip = {},
                )
            }
        }
        val row = compose.onNodeWithText("첫 책")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)).assertIsOff()
        row.performClick().assertIsOn()
        row.performClick().assertIsOff()
    }
}
