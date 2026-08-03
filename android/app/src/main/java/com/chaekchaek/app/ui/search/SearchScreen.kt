package com.chaekchaek.app.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
  val search = { viewModel.search(query) }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
    Spacer(Modifier.height(22.dp))
    Text("MY READING DESK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    Text("새 책을 찾아볼까요?", style = MaterialTheme.typography.headlineLarge)
    Text("제목이나 저자를 검색해 내 서재에 기록하세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))

    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
      Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.weight(1f),
          placeholder = { Text("책 제목, 저자로 검색") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { search() }),
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          ),
        )
        Spacer(Modifier.width(8.dp))
        Button(
          onClick = search,
          shape = RoundedCornerShape(12.dp),
          contentPadding = ButtonDefaults.ContentPadding,
        ) { Text("검색", style = MaterialTheme.typography.labelSmall) }
      }
    }
    Spacer(Modifier.height(20.dp))

    when (val s = state) {
      SearchUiState.Idle -> SearchMessage("첫 번째 책을\n서재에 올려 보세요.")
      SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
      is SearchUiState.Error -> SearchMessage("검색 결과를 가져오지 못했어요.\n잠시 후 다시 시도해 주세요.")
      is SearchUiState.Success ->
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          item { Text("SEARCH RESULTS · ${s.results.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
          items(s.results) { book ->
            Surface(
              modifier = Modifier.fillMaxWidth().clickable { selectedBook = book },
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
              Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(width = 48.dp, height = 64.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                  Box(contentAlignment = Alignment.Center) { Text("책", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Spacer(Modifier.height(4.dp))
                  Text("${book.creator} · ${book.publisher}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(book.year, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
    }
  }

  selectedBook?.let { book ->
    BookRegisterDialog(
      book = book,
      onDismiss = { selectedBook = null },
      onRegister = { title, creator, publisher, year, note ->
        archiveRepository.add(ArchivedBook(UUID.randomUUID().toString(), title, creator, publisher, year, book.coverUrl, note))
        selectedBook = null
      },
    )
  }
}

@Composable
private fun SearchMessage(message: String) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
      Text(message, modifier = Modifier.padding(28.dp), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
