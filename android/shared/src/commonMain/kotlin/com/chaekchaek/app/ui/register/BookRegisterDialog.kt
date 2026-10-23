package com.chaekchaek.app.ui.register

import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog

@Composable
fun BookRegisterDialog(
    book: BookSearchResult,
    onDismiss: () -> Unit,
    onRegister: (title: String, creator: String, publisher: String, year: String, note: String) -> Unit,
) {
    val errorReporter = com.chaekchaek.app.analytics.LocalAnalyticsTracker.current.errors
    val errors = remember(errorReporter) { com.chaekchaek.app.presentation.common.ErrorRecoveryState(errorReporter) }
    com.chaekchaek.app.ui.common.ErrorRecoveryEffect(errors)
    val titleState = rememberLimitedTextFieldState(BookRegisterInputRules.TitlePolicy, book.title, book)
    val title = titleState.text.toString()
    val creatorState = rememberLimitedTextFieldState(BookRegisterInputRules.CreatorPolicy, book.creator, book)
    val creator = creatorState.text.toString()
    val publisherState = rememberLimitedTextFieldState(BookRegisterInputRules.PublisherPolicy, book.publisher, book)
    val publisher = publisherState.text.toString()
    val yearState = rememberLimitedTextFieldState(BookRegisterInputRules.YearPolicy, book.year, book)
    val year = yearState.text.toString()
    val noteState = rememberLimitedTextFieldState(BookRegisterInputRules.NotePolicy, "", book)
    val note = noteState.text.toString()
    val editedBook = book.copy(title = title.trim(), creator = creator.trim(), publisher = publisher.trim(), year = year.trim())

    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("ADD TO ARCHIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("책을 서재에 올리기", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BookField(titleState, BookRegisterInputRules.TitlePolicy, "제목")
                BookField(creatorState, BookRegisterInputRules.CreatorPolicy, "저자")
                BookField(publisherState, BookRegisterInputRules.PublisherPolicy, "출판사")
                BookField(yearState, BookRegisterInputRules.YearPolicy, "연도")
                BookField(noteState, BookRegisterInputRules.NotePolicy, "한줄평 - 선택")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val error = editedBook.registrationValidationError()
                    if (error != null) errors.confirm(error)
                    else onRegister(editedBook.title, editedBook.creator, editedBook.publisher, editedBook.year, note.trim())
                },
            ) {
                Text("서재에 등록", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

internal object BookRegisterInputRules {
    const val MAX_TITLE_LENGTH = 300
    const val MAX_CREATOR_LENGTH = 100
    const val MAX_PUBLISHER_LENGTH = 100
    const val MAX_YEAR_LENGTH = 4
    const val MAX_NOTE_LENGTH = 1000

    val TitlePolicy = TextInputPolicy(MAX_TITLE_LENGTH)
    val CreatorPolicy = TextInputPolicy(MAX_CREATOR_LENGTH)
    val PublisherPolicy = TextInputPolicy(MAX_PUBLISHER_LENGTH)
    val YearPolicy = TextInputPolicy(MAX_YEAR_LENGTH, digitsOnly = true)
    val NotePolicy = TextInputPolicy(MAX_NOTE_LENGTH)

    fun limitTitle(value: String): String = TitlePolicy.limit(value)

    fun changeTitle(current: String, proposed: String): String =
        TitlePolicy.applyChange(current, proposed)

    fun limitCreator(value: String): String = CreatorPolicy.limit(value)

    fun changeCreator(current: String, proposed: String): String =
        CreatorPolicy.applyChange(current, proposed)

    fun limitPublisher(value: String): String = PublisherPolicy.limit(value)

    fun changePublisher(current: String, proposed: String): String =
        PublisherPolicy.applyChange(current, proposed)

    fun limitYear(value: String): String = YearPolicy.limit(value)

    fun changeYear(current: String, proposed: String): String =
        YearPolicy.applyChange(current, proposed)

    fun limitNote(value: String): String = NotePolicy.limit(value)

    fun changeNote(current: String, proposed: String): String =
        NotePolicy.applyChange(current, proposed)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BookField(state: TextFieldState, policy: TextInputPolicy, label: String) {
    OutlinedTextField(
        state = state,
        inputTransformation = policy.inputTransformation,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        lineLimits = TextFieldLineLimits.MultiLine(),
    )
}
