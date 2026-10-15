package com.chamsae.chaekchaek

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.search.SearchScreen
import com.chaekchaek.app.ui.search.SearchUiState
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class TextInputSafetyTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchLimitsImeAndClipboardAndRestoresBoundedDraft() {
        val query = mutableStateOf("")
        lateinit var clipboard: ClipboardManager
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val manager = LocalClipboardManager.current
            SideEffect { clipboard = manager }
            ChaekchaekTheme {
                SearchScreen(
                    state = SearchUiState.Idle, sort = BookSearchSort.LATEST,
                    query = query.value, recentSearches = emptyList(), registeredBookIds = emptySet(),
                    searchResultsListState = rememberLazyListState(), onSearch = {},
                    onQueryChange = { query.value = it }, onClear = {}, onRegister = {},
                    onLoadMore = {}, onSortSelect = {},
                )
            }
        }
        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("가".repeat(10_000))
        field.assertTextEquals("가".repeat(100))
        field.performTextClearance()
        compose.runOnIdle { clipboard.setText(AnnotatedString("📚".repeat(10_000))) }
        field.performClick().performSemanticsAction(SemanticsActions.PasteText) { it() }
        field.assertTextEquals("📚".repeat(100))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextEquals("📚".repeat(100))
        compose.runOnIdle { assertEquals("📚".repeat(100), query.value) }
    }

    @Test
    fun reviewBoundsEmojiBeforeLayoutAndPreservesDraftOnRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = BookDetailUiState(
                        book = BookDetailArgs(id = "42", bookId = 42, title = "입력 안전성 검증"),
                        signedIn = true,
                    ),
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("감상 남기기").performClick()
        val field = compose.onNodeWithContentDescription("감상 (필수)")
        field.performTextInput("가".repeat(999) + "📚" + "초과".repeat(10_000))
        field.assertTextEquals("가".repeat(999))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("감상 (필수)").assertTextEquals("가".repeat(999))
    }
}
