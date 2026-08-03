package com.chaekchaek.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chaekchaek.app.data.BookSearchResult

/** 검색 결과 클릭 시 뜨는 팝업. 필드를 확인·수정한 뒤 등록한다. */
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
    title = { Text("책 정보 확인") },
    text = {
      Column {
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = creator, onValueChange = { creator = it }, label = { Text("저자") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = publisher,
          onValueChange = { publisher = it },
          label = { Text("출판사") },
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("연도") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("한줄평 (선택)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(onClick = { onRegister(title, creator, publisher, year, note) }) { Text("등록") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
  )
}
