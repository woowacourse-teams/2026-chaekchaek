package com.chamsae.chaekchaek

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailUiState
import com.chaekchaek.app.ui.common.LibrarySaveButton
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibrarySaveInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun savedButtonConsumesTapWithoutNavigatingOrSavingAgain() {
        val saved = mutableStateOf(false)
        var saves = 0
        var navigations = 0
        compose.setContent {
            ChaekchaekTheme {
                Box(Modifier.clickable { navigations++ }) {
                    LibrarySaveButton("책", saved.value, onSave = { saves++; saved.value = true })
                }
            }
        }
        compose.onNodeWithContentDescription("책, 서재에 담기").assertHeightIsAtLeast(44.dp).performClick()
        compose.onNodeWithContentDescription("책, 담았어요").assertIsDisplayed().performClick().performClick()
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals(0, navigations)
        }
    }

    @Test
    fun savingButtonIsDisabledAndKeepsCardInPlace() {
        var saves = 0
        var navigations = 0
        compose.setContent {
            ChaekchaekTheme {
                Box(Modifier.clickable { navigations++ }) {
                    LibrarySaveButton("책", false, onSave = { saves++ }, saving = true)
                }
            }
        }
        compose.onNodeWithContentDescription("책, 담는 중").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(0, saves)
            assertEquals(0, navigations)
        }
    }

    @Test
    fun detailRequiresSeparateMenuAndConfirmationToDelete() {
        val saved = mutableStateOf(true)
        var adds = 0
        var deletes = 0
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = BookDetailUiState(book = BookDetailArgs("42", title = "검증 도서"), signedIn = true),
                    savedToLibrary = saved.value,
                    onBack = {},
                    onAddToLibrary = { adds++ },
                    onRemoveFromLibrary = { deletes++; saved.value = false },
                )
            }
        }
        compose.onNodeWithContentDescription("검증 도서, 담았어요").performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithContentDescription("서재 관리").performClick()
        compose.onNodeWithText("서재에서 삭제").performClick()
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithContentDescription("서재 관리").performClick()
        compose.onNodeWithText("서재에서 삭제").performClick()
        compose.onNodeWithText("삭제", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertEquals(1, deletes)
            assertEquals(0, adds)
        }
        compose.onNodeWithContentDescription("검증 도서, 서재에 담기").assertIsDisplayed()
    }
}
