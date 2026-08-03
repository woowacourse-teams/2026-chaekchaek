package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.chaekchaek.app.data.ArchiveRepository

@Composable
fun ArchiveScreen(archiveRepository: ArchiveRepository, modifier: Modifier = Modifier) {
  val items by archiveRepository.items.collectAsState()

  if (items.isEmpty()) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("등록된 책이 없습니다") }
    return
  }

  LazyColumn(modifier = modifier.fillMaxSize()) {
    items(items, key = { it.id }) { book ->
      ListItem(
        headlineContent = { Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
          Text("${book.creator} · ${book.publisher} · ${book.year}", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}
