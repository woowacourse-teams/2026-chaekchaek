package com.chaekchaek.app.ui.publiclibrary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_back
import com.chaekchaek.app.domain.library.*
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.ui.RemoteBookImage
import com.chaekchaek.app.ui.archive.ArchiveBookUiModel
import com.chaekchaek.app.ui.archive.BookshelfSpineRow
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.common.ChaekFilterChip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun PublicLibraryScreen(
    ownerName: String, state: PublicReadState<PublicLibrary>, onBack: () -> Unit,
    onBookshelf: (PublicBookshelf) -> Unit, modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    PublicLibraryLayout(ownerName, state.showLoading, onBack, modifier) {
        // 로딩 중 임시 목록을 배치하면 복원할 스크롤 위치가 0으로 덮어써진다.
        state.content?.let { library ->
            if (library.libraries.isEmpty()) EmptyLibrary("아직 담은 책이 없어요")
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(library.libraries, key = { it.libraryId }) { shelf ->
                    Column(Modifier.fillMaxWidth().clickable(role = Role.Button) { onBookshelf(shelf) }.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(shelf.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            Text("${shelf.totalCount}권", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                        if (shelf.items.isEmpty()) Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("아직 담은 책이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else BookshelfSpineRow(shelf.items.map { book ->
                            ArchiveBookUiModel(book.isbn13, book.bookId, book.title, "", "", "", "", book.spineImageUrl, ReadingStatus.WANT_TO_READ, 0, 0, 0)
                        }, onBook = { onBookshelf(shelf) })
                        HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
internal fun PublicBookshelfScreen(
    ownerName: String, state: PublicReadState<PublicBookshelfDetail>, onBack: () -> Unit,
    onBook: (BookDetailArgs) -> Unit, modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    PublicLibraryLayout("책장", state.showLoading, onBack, modifier) {
        state.content?.let { shelf ->
            val books = shelf.items.filter { filter == null || it.status == filter }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)) {
                item(key = "intro") {
                    Text(shelf.name, style = MaterialTheme.typography.headlineSmall)
                    Text("$ownerName   ${shelf.totalCount}권", modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(24.dp))
                    if (shelf.items.isNotEmpty()) {
                        BookshelfSpineRow(shelf.items.map { book ->
                            ArchiveBookUiModel(book.isbn13, book.bookId, book.title, book.authors.joinToString(", "), "", "", book.coverImageUrl, book.spineImageUrl, ReadingStatus.WANT_TO_READ, book.currentPage, book.totalPages ?: 0, 0)
                        }, onBook = { book -> shelf.items.firstOrNull { it.bookId == book.bookId }?.let { onBook(it.toDetailArgs()) } })
                        HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(null to "전체", "WANT_TO_READ" to "읽고 싶어요", "READING" to "읽는 중", "FINISHED" to "다 읽음").forEach { (value, label) ->
                            ChaekFilterChip(label, filter == value, onClick = {
                                if (filter != value) { filter = value; scope.launch { listState.scrollToItem(0) } }
                            })
                        }
                    }
                }
                if (books.isEmpty()) item(key = "empty") {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        Text(if (shelf.items.isEmpty()) "아직 담은 책이 없어요" else "이 독서 상태의 책이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                items(books, key = { it.bookId }) { book ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { onBook(book.toDetailArgs()) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RemoteBookImage(book.coverImageUrl, "${book.title} 표지", Modifier.size(40.dp, 58.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(book.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (book.authors.isNotEmpty()) Text(book.authors.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(book.readingLabel(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicLibraryLayout(title: String, loading: Boolean, onBack: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().height(58.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)) {
                Icon(painterResource(Res.drawable.ic_back), "뒤로 가기", Modifier.size(21.dp))
            }
            Text(title, Modifier.align(Alignment.Center).padding(horizontal = 72.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
            if (loading) CircularProgressIndicator(Modifier.size(20.dp).align(Alignment.Center), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun EmptyLibrary(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

internal fun PublicLibraryBook.toDetailArgs() = BookDetailArgs(
    id = isbn13, isbn13 = isbn13, bookId = bookId, title = title, creator = authors.joinToString(", "),
    totalPages = totalPages ?: 0, coverUrl = coverImageUrl,
)
internal fun PublicLibraryBook.readingLabel(): String {
    val label = when (status) { "WANT_TO_READ" -> "읽고 싶어요"; "READING" -> "읽는 중"; "FINISHED" -> "다 읽음"; else -> "독서 상태 없음" }
    return "$label  ${currentPage}쪽" + (totalPages?.let { " / ${it}쪽" } ?: "")
}
