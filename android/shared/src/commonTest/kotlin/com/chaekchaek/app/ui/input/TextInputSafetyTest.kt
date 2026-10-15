package com.chaekchaek.app.ui.input

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextRange
import com.chaekchaek.app.ui.archive.NicknameInputPolicy
import com.chaekchaek.app.ui.bookdetail.BookDetailInputRules
import com.chaekchaek.app.ui.bookdetail.ReplyInputRules
import com.chaekchaek.app.ui.register.BookRegisterInputRules
import com.chaekchaek.app.ui.search.SearchInputRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextInputSafetyTest {
    private val policies = listOf(
        SearchInputRules.Policy, NicknameInputPolicy,
        BookDetailInputRules.ContentPolicy, BookDetailInputRules.QuotePolicy,
        BookDetailInputRules.ChapterPolicy, BookDetailInputRules.PagePolicy, ReplyInputRules.Policy,
        BookRegisterInputRules.TitlePolicy, BookRegisterInputRules.CreatorPolicy,
        BookRegisterInputRules.PublisherPolicy, BookRegisterInputRules.YearPolicy, BookRegisterInputRules.NotePolicy,
    )

    @Test
    fun everyFieldBoundsLargeInputAndLegacyRestoration() {
        policies.forEach { policy ->
            val oversized = (if (policy.digitsOnly) "1" else "한").repeat(100_000)
            val expected = oversized.take(policy.maximum)
            assertEquals(expected, policy.applyChange("", oversized))
            val restored = assertNotNull(policy.saver.restore(oversized))
            assertEquals(expected, restored.text.toString())
            assertEquals(TextRange(expected.length), restored.selection)
        }
    }

    @Test
    fun middlePasteKeepsExistingSuffixAndAcceptsOnlyRemainingRoom() {
        val policy = TextInputPolicy(6, utf16 = true)
        assertEquals("앞📚ab뒤", policy.applyChange("앞뒤", "앞📚abcdefghi뒤"))
        assertEquals("앞📚ab뒤", policy.applyChange("앞📚ab뒤", "앞📚ab" + "가".repeat(100_000) + "뒤"))
    }

    @Test
    fun utf16LimitsNeverLeaveHalfOfAnEmoji() {
        assertEquals("한", TextInputPolicy(2, utf16 = true).limit("한📚"))
        assertEquals("📚", TextInputPolicy(2, utf16 = true).limit("📚가"))
        assertEquals("한📚", TextInputPolicy(2).limit("한📚가"))
        assertEquals("한글", TextInputPolicy(10).limit("한\uD83D글\uDCDA"))
    }

    @Test
    fun initialAndRestoredNumericValuesUseTheSameFilter() {
        val policy = BookDetailInputRules.PagePolicy
        assertEquals("1234567", policy.limit("쪽1a234567890"))
        assertEquals("1234567", policy.restoredState("쪽1a234567890", 20, -1).text.toString())
        assertEquals("2026", BookRegisterInputRules.YearPolicy.applyChange("20", "20년26월99"))
    }

    @Test
    fun restorationClampsSelectionAndKeepsItOutsideSurrogatePairs() {
        val state = TextInputPolicy(3).restoredState("한📚끝초과", 2, 100)
        assertEquals("한📚끝", state.text.toString())
        assertEquals(TextRange(1, 4), state.selection)
    }

    @Test
    fun savingProgrammaticOversizedStateCannotGrowSavedState() {
        val policy = TextInputPolicy(10)
        val raw = TextFieldState("📚".repeat(100_000), TextRange(199_999))
        val saved = with(policy.saver) { SaverScope { true }.save(raw) }
        val restored = assertNotNull(policy.saver.restore(assertNotNull(saved)))
        assertEquals("📚".repeat(10), restored.text.toString())
        assertEquals(TextRange(20), restored.selection)
    }

    @Test
    fun allTextPoliciesReturnWellFormedUnicodeWithinTheirExistingLimits() {
        policies.filterNot { it.digitsOnly }.forEach { policy ->
            val accepted = policy.applyChange("", "가📚\uD800".repeat(10_000))
            assertTrue(if (policy.utf16) accepted.length <= policy.maximum else accepted.length <= policy.maximum * 2)
            assertFalse(Regex("[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]").containsMatchIn(accepted))
        }
    }

    @Test
    fun repeatedPrefixPastePreservesExistingTextAndAcceptedCursor() {
        val state = TextFieldState("aaaa", TextRange.Zero)
        edit(state, TextInputPolicy(5), 0, 0, "aX", TextRange(2))
        assertEquals("aaaaa", state.text.toString())
        assertEquals(TextRange(1), state.selection)
    }

    @Test
    fun fullyRejectedRepeatedPasteKeepsCursorAtInsertionPoint() {
        val state = TextFieldState("1111", TextRange.Zero)
        edit(state, TextInputPolicy(4), 0, 0, "111", TextRange(3))
        assertEquals("1111", state.text.toString())
        assertEquals(TextRange.Zero, state.selection)
    }

    @Test
    fun selectionReplacementKeepsAcceptedTextEvenWhenSuffixRepeats() {
        val state = TextFieldState("가".repeat(100), TextRange(0, 1))
        edit(state, TextInputPolicy(100), 0, 1, "나가", TextRange(2))
        assertEquals("나" + "가".repeat(99), state.text.toString())
        assertEquals(TextRange(1), state.selection)
    }

    @Test
    fun middlePasteMatchingExistingSuffixStaysAtActualInsertionPoint() {
        val state = TextFieldState("abcd", TextRange(2))
        edit(state, TextInputPolicy(5), 2, 2, "cdXYZ", TextRange(7))
        assertEquals("abccd", state.text.toString())
        assertEquals(TextRange(3), state.selection)
    }

    @Test
    fun multipleEditsShareRemainingRoomAndPreserveReversedSelection() {
        val state = TextFieldState("abcd")
        state.edit {
            replace(1, 1, "XX")
            replace(5, 5, "YY")
            selection = TextRange(8, 0)
            with(TextInputPolicy(6).inputTransformation) { transformInput() }
        }
        assertEquals("aXXbcd", state.text.toString())
        assertEquals(TextRange(6, 0), state.selection)
    }

    @Test
    fun numericFilteringMapsBothSelectionEndpointsThroughRejectedCharacters() {
        val state = TextFieldState("1234", TextRange(2))
        edit(state, TextInputPolicy(5, digitsOnly = true), 2, 2, "a9bc", TextRange(6, 2))
        assertEquals("12934", state.text.toString())
        assertEquals(TextRange(3, 2), state.selection)
    }

    @Test
    fun partialSurrogateDeletionCannotLeaveAnUnpairedCharacter() {
        val state = TextFieldState("📚가", TextRange(1, 2))
        edit(state, TextInputPolicy(3), 1, 2, "", TextRange(1))
        assertEquals("가", state.text.toString())
        assertEquals(TextRange.Zero, state.selection)
    }

    @Test
    fun everyFieldTransformationBoundsLargePasteBeforeItEntersState() {
        policies.forEach { policy ->
            val text = (if (policy.digitsOnly) "1" else "한").repeat(100_000)
            val state = TextFieldState()
            edit(state, policy, 0, 0, text, TextRange(text.length))
            assertEquals(policy.limit(text), state.text.toString())
            assertEquals(TextRange(state.text.length), state.selection)
        }
    }

    @Test
    fun transformationsExposeMaximumLengthToAccessibility() {
        policies.forEach { policy ->
            val semantics = SemanticsConfiguration()
            with(policy.inputTransformation) { semantics.applySemantics() }
            assertEquals(policy.maximum, semantics[SemanticsProperties.MaxTextLength])
        }
    }

    private fun edit(
        state: TextFieldState,
        policy: TextInputPolicy,
        start: Int,
        end: Int,
        text: String,
        selectionAfterEdit: TextRange,
    ) {
        state.edit {
            replace(start, end, text)
            selection = selectionAfterEdit
            with(policy.inputTransformation) { transformInput() }
        }
    }
}
