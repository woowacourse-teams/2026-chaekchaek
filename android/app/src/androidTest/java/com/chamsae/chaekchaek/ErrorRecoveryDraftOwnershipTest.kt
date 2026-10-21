@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.chamsae.chaekchaek

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.ui.archive.MemberSettingsUiState
import com.chaekchaek.app.ui.archive.MyPageScreen
import com.chaekchaek.app.ui.bookdetail.ReplyInputSheet
import com.chaekchaek.app.ui.bookdetail.PageInputDialog
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorRecoveryDraftOwnershipTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nicknameDraftRestorationWaitsForAnActualSaveRevision() {
        val state = mutableStateOf(MemberSettingsUiState(signedIn = true))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ChaekchaekTheme {
                MyPageScreen(state.value, {}, { _, _ -> }, {}, {})
            }
        }
        compose.onNodeWithText("익명으로 감상 공개").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("보존할닉네임")
        compose.onNodeWithText("확인").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("닉네임 설정").assertExists()
        compose.onNode(hasSetTextAction()).assertTextEquals("보존할닉네임")
        compose.runOnIdle { state.value = state.value.copy(saveRevision = 1) }
        compose.onNodeWithText("닉네임 설정").assertDoesNotExist()
    }

    @Test
    fun nicknameSuccessKeepsEditsMadeAfterSubmission() {
        val state = mutableStateOf(MemberSettingsUiState(signedIn = true))
        compose.setContent {
            ChaekchaekTheme { MyPageScreen(state.value, {}, { _, _ -> }, {}, {}) }
        }
        compose.onNodeWithText("익명으로 감상 공개").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("첫닉네임")
        compose.onNodeWithText("확인").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("새닉네임")
        compose.runOnIdle { state.value = state.value.copy(saveRevision = 1) }
        compose.onNode(hasSetTextAction()).assertTextEquals("새닉네임")
        compose.onNodeWithText("확인").performClick()
        compose.runOnIdle { state.value = state.value.copy(saveRevision = 2) }
        compose.onNodeWithText("닉네임 설정").assertDoesNotExist()
    }

    @Test
    fun pageSuccessKeepsEditsMadeAfterSubmission() = verifyPageCompletion(reopen = false)

    @Test
    fun pageSuccessCannotCloseANewDialogInstance() = verifyPageCompletion(reopen = true)

    private fun verifyPageCompletion(reopen: Boolean) {
        val visible = mutableStateOf(true)
        var complete: (() -> Unit)? = null
        compose.setContent {
            ChaekchaekTheme {
                if (visible.value) PageInputDialog(
                    initialPage = 0, totalPages = 200,
                    onDismiss = { visible.value = false },
                    onSave = { _, onSaved -> complete = onSaved },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("10")
        compose.onNodeWithText("저장").performClick()
        if (reopen) {
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            compose.runOnIdle { visible.value = true }
        }
        val draft = if (reopen) "10" else "20"
        compose.onNode(hasSetTextAction()).performTextReplacement(draft)
        compose.runOnIdle { checkNotNull(complete).invoke() }
        compose.onNode(hasSetTextAction()).assert(SemanticsMatcher.expectValue(
            SemanticsProperties.EditableText, AnnotatedString(draft)))
        compose.onNodeWithText("저장").performClick()
        compose.runOnIdle { checkNotNull(complete).invoke() }
        compose.onNodeWithText("어디까지 읽으셨나요?").assertDoesNotExist()
    }

    @Test
    fun replySuccessKeepsEditsMadeAfterSubmission() = verifyReplyCompletion(reopen = false)

    @Test
    fun replySuccessCannotCloseANewComposerInstance() = verifyReplyCompletion(reopen = true)

    private fun verifyReplyCompletion(reopen: Boolean) {
        val visible = mutableStateOf(true)
        var complete: (() -> Unit)? = null
        compose.setContent {
            ChaekchaekTheme {
                if (visible.value) ReplyInputSheet(
                    onDismiss = { visible.value = false },
                    onSave = { _, onSaved -> complete = onSaved },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("제출한 답글")
        compose.onNodeWithText("등록").performScrollTo().performClick()
        if (reopen) {
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            compose.runOnIdle { visible.value = true }
        }
        val draft = if (reopen) "제출한 답글" else "아직 저장하지 않은 새 초안"
        compose.onNode(hasSetTextAction()).performTextReplacement(draft)
        compose.runOnIdle { checkNotNull(complete).invoke() }
        compose.onNode(hasSetTextAction()).assertTextEquals(draft)
        compose.onNodeWithText("등록").performScrollTo().performClick()
        compose.runOnIdle { checkNotNull(complete).invoke() }
        compose.onNodeWithText("답글 작성").assertDoesNotExist()
    }
}
