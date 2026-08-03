package com.chaekchaek.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.chaekchaek.app.data.BookSearchResult

@Composable
fun BookRegisterDialog(
  book: BookSearchResult,
  onDismiss: () -> Unit,
  onRegister: (title: String, creator: String, publisher: String, year: String, note: String) -> Unit,
) {
  var title by remember(book) { mutableStateOf(book.title) }
  var creator by remember(book) { mutableStateOf(book.creator) }
  var publisher by remember(book) { mutableStateOf(book.publisher) }
  var year by remember(book) { mutableStateOf(book.year) }
  var note by remember(book) { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
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
        BookField(title, { title = it }, "제목")
        BookField(creator, { creator = it }, "저자")
        BookField(publisher, { publisher = it }, "출판사")
        BookField(year, { year = it }, "연도")
        BookField(note, { note = it }, "한줄평 - 선택")
      }
    },
    confirmButton = {
      TextButton(onClick = { onRegister(title, creator, publisher, year, note) }) { Text("서재에 등록", color = MaterialTheme.colorScheme.primary) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
  )
}

@Composable
private fun BookField(value: String, onValueChange: (String) -> Unit, label: String) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
  )
}
