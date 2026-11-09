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
import com.chaekchaek.app.data.remote.BookDetail
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
    fun savedButtonRequestsShelfChoiceWithoutNavigating() {
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
            assertEquals(3, saves)
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
    fun detailSavedBookButtonAndOtherShelfMenuDoNotDelete() {
        var adds = 0
        var deletes = 0
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = savedDetailState(),
                    savedToLibrary = true,
                    onBack = {},
                    onAddToLibrary = { adds++ },
                    onRemoveFromLibrary = { deletes++ },
                )
            }
        }
        compose.onNodeWithContentDescription("검증 도서, 담았어요").performClick()
        compose.onNodeWithContentDescription("서재 관리").performClick()
        compose.onNodeWithText("다른 책장에 담기").performClick()
        compose.runOnIdle {
            assertEquals(2, adds)
            assertEquals(0, deletes)
        }
        compose.onNodeWithContentDescription("검증 도서, 담았어요").assertIsDisplayed()
    }
    @Test
    fun detailOpenedFromShelfSearchHidesOtherShelfMenuAndKeepsPrimarySave() {
        var adds = 0
        compose.setContent {
            ChaekchaekTheme {
                BookDetailScreen(
                    state = savedDetailState(),
                    savedToLibrary = true,
                    showOtherShelfMenu = false,
                    onBack = {},
                    onAddToLibrary = { adds++ },
                )
            }
        }
        compose.onNodeWithContentDescription("서재 관리").assertDoesNotExist()
        compose.onNodeWithText("다른 책장에 담기").assertDoesNotExist()
        compose.onNodeWithContentDescription("검증 도서, 담았어요").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, adds) }
    }

    private fun savedDetailState() = BookDetailUiState(
        book = BookDetailArgs("42", title = "검증 도서"),
        signedIn = true,
        detail = BookDetail(
            bookId = 42, isbn13 = "42", title = "검증 도서", authors = emptyList(),
            translators = emptyList(), publisher = "", category = "", publishedDate = null,
            totalPages = 100, coverImageUrl = "", commentCount = 0, averageRating = null,
            ratingCount = 0, myRecord = null, isRegisteredInMyLibrary = true,
        ),
    )
}
