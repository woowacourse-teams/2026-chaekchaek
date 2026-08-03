package com.chaekchaek.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaekchaek.app.data.ArchiveRepository
import com.chaekchaek.app.data.ArchivedBook
import com.chaekchaek.app.data.BookSearchResult
import java.util.UUID

@Composable
fun SearchScreen(
  archiveRepository: ArchiveRepository,
  modifier: Modifier = Modifier,
  viewModel: SearchViewModel = viewModel(),
) {
  val state by viewModel.uiState.collectAsState()
  var query by remember { mutableStateOf("") }
  var selectedBook by remember { mutableStateOf<BookSearchResult?>(null) }

  Column(modifier = modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.weight(1f),
        label = { Text("책 제목, 저자로 검색") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
      )
      Button(onClick = { viewModel.search(query) }) { Text("검색") }
    }

    when (val s = state) {
      SearchUiState.Idle -> Text("검색어를 입력하세요")
      SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is SearchUiState.Error -> Text("오류: ${s.message}")
      is SearchUiState.Success ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          items(s.results) { book ->
            ListItem(
              headlineContent = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
              supportingContent = {
                Text("${book.creator} · ${book.publisher} · ${book.year}", maxLines = 1, overflow = TextOverflow.Ellipsis)
              },
              modifier = Modifier.fillMaxWidth().clickable { selectedBook = book },
            )
          }
        }
    }
  }

  selectedBook?.let { book ->
    BookRegisterDialog(
      book = book,
      onDismiss = { selectedBook = null },
      onRegister = { title, creator, publisher, year, note ->
        archiveRepository.add(
          ArchivedBook(
            id = UUID.randomUUID().toString(),
            title = title,
            creator = creator,
            publisher = publisher,
            year = year,
            coverUrl = book.coverUrl,
            note = note,
          )
        )
        selectedBook = null
      },
    )
  }
}
