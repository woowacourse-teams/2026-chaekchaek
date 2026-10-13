package com.chaekchaek.app.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var title by remember(book) { mutableStateOf(BookRegisterInputRules.limitTitle(book.title)) }
    var creator by remember(book) { mutableStateOf(BookRegisterInputRules.limitCreator(book.creator)) }
    var publisher by remember(book) { mutableStateOf(BookRegisterInputRules.limitPublisher(book.publisher)) }
    var year by remember(book) { mutableStateOf(BookRegisterInputRules.limitYear(book.year)) }
    var note by remember(book) { mutableStateOf("") }
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
                BookField(title, { title = BookRegisterInputRules.limitTitle(it) }, "제목")
                BookField(creator, { creator = BookRegisterInputRules.limitCreator(it) }, "저자")
                BookField(publisher, { publisher = BookRegisterInputRules.limitPublisher(it) }, "출판사")
                BookField(year, { year = BookRegisterInputRules.limitYear(it) }, "연도")
                BookField(note, { note = BookRegisterInputRules.limitNote(it) }, "한줄평 - 선택")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRegister(editedBook.title, editedBook.creator, editedBook.publisher, editedBook.year, note.trim()) },
                enabled = editedBook.registrationValidationError() == null,
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

    fun limitTitle(value: String): String = value.take(MAX_TITLE_LENGTH)

    fun limitCreator(value: String): String = value.take(MAX_CREATOR_LENGTH)

    fun limitPublisher(value: String): String = value.take(MAX_PUBLISHER_LENGTH)

    fun limitYear(value: String): String = value.filter(Char::isDigit).take(MAX_YEAR_LENGTH)

    fun limitNote(value: String): String = value.take(MAX_NOTE_LENGTH)
}

@Composable
private fun BookField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = label != "한줄평 - 선택",
    )
}
