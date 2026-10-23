package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import com.chaekchaek.app.data.remote.*
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.common.*
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import com.chaekchaek.app.ui.theme.*

@Composable
internal fun BookshelfRoute(
    viewModel: BookshelfViewModel,
    signedIn: Boolean,
    onProfile: () -> Unit,
    onLogin: () -> Unit,
    scrollTopRequest: Int,
    onBook: (BookDetailArgs) -> Unit,
    onAdd: (BookDetailArgs) -> Unit,
    token: String?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    var namingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Bookshelf?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var sharing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(signedIn, state.memberId) { creating = false; namingId = null; deleting = null; adding = false; sharing = false; editing = false }
    val listState = rememberLazyListState()
    val detailListState = rememberLazyListState()
    LaunchedEffect(scrollTopRequest) { if (state.selectedId == null) listState.scrollToItem(0) else detailListState.scrollToItem(0) }
    val canOrder = !state.mutating && !state.refreshing && !state.collectionStale && !state.detailRefreshing &&
        (state.selectedId == null || !state.detailStale)
    val detail = state.detail
    val selected = state.shelves.firstOrNull { it.libraryId == state.selectedId }
    val back = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(back, isBackEnabled = state.selectedId != null) { viewModel.close(); editing = false }
    Column(modifier.fillMaxSize().background(ChaekSurface)) {
        if (state.selectedId == null) BrandHeader(onProfile) else Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.close(); editing = false }) { Text("‹", fontFamily = ChaekIconFontFamily()) }
            Text("책 플레이리스트", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            selected?.let { shelf -> ShelfMenu(shelf, { namingId = shelf.libraryId }, { deleting = shelf }) }
        }
        if (!signedIn) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("나만의 책장을 만들어 보세요", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                ChaekOverlayButton("로그인", onLogin)
            }
        } else if (state.selectedId == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("나의 책장", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { creating = true }, enabled = !state.mutating, modifier = Modifier.testTag("bookshelf-create")) { Text("+ 책장") }
            }
            Text("책은 꾹 눌러 이동, 넘치는 책은 좌우로 밀어 보기", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
            ChaekRefreshLayout(
                com.chaekchaek.app.presentation.common.RefreshUiState(isRefreshing = state.refreshing, enabled = !state.mutating),
                viewModel::refresh, Modifier.weight(1f),
            ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState) {
                itemsIndexed(state.shelves, key = { _, it -> it.libraryId }) { index, shelf ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReorderHandle(index, state.shelves.size, canOrder, "bookshelf-reorder-${shelf.libraryId}", 340f) { from, to ->
                                viewModel.orderShelves(state.shelves.map { it.libraryId }.moved(from, to))
                            }
                            TextButton(onClick = { viewModel.open(shelf.libraryId) }, modifier = Modifier.weight(1f).testTag("bookshelf-${shelf.libraryId}")) {
                                Text(shelf.name, Modifier.weight(1f), color = ChaekInk, style = MaterialTheme.typography.titleLarge)
                            }
                            Text("${shelf.totalCount}권", style = MaterialTheme.typography.bodySmall)
                            ShelfMenu(shelf, { namingId = shelf.libraryId }, { deleting = shelf })
                        }
                        SpineRow(shelf.items.map { it.asArchive() }, onBook = { onBook(it.asArgs()) }, onMove = if (!canOrder) null else { from, to ->
                            viewModel.orderBooks(shelf.libraryId, shelf.items.map { it.bookId }.moved(from, to))
                        })
                        HorizontalDivider(color = ChaekBorder)
                    }
                }
                item { TextButton(onClick = viewModel::refresh, modifier = Modifier.fillMaxWidth(), enabled = !state.refreshing) { Text("새로고침") } }
            }
            }
        } else if (detail != null) {
            ChaekRefreshLayout(
                com.chaekchaek.app.presentation.common.RefreshUiState(isRefreshing = state.refreshing || state.detailRefreshing, enabled = !state.mutating && !editing),
                viewModel::refresh, Modifier.weight(1f),
            ) {
            LazyColumn(Modifier.fillMaxSize(), state = detailListState, contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(detail.name, style = MaterialTheme.typography.headlineSmall)
                    Text("${detail.totalCount}권", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
                }
                item { SpineRow(detail.items.map { it.asArchive() }, onBook = { onBook(it.asArgs()) }, onMove = if (!canOrder) null else { from, to ->
                    viewModel.orderBooks(detail.libraryId, detail.items.map { it.bookId }.moved(from, to))
                }) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChaekOverlayButton("+ 책 담기", { adding = true }, modifier = Modifier.weight(1f), enabled = !state.mutating)
                        ChaekOverlayButton("이미지 공유", { sharing = true }, enabled = !state.mutating && !state.refreshing && !state.detailRefreshing && !state.detailStale, secondary = true, modifier = Modifier.weight(1f))
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("이 책장에 담긴 책", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "완료" else "편집") }
                } }
                itemsIndexed(detail.items, key = { _, it -> it.bookId }) { index, book ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (!editing) ReorderHandle(index, detail.items.size, canOrder, "bookshelf-book-reorder-${book.bookId}") { from, to ->
                            viewModel.orderBooks(detail.libraryId, detail.items.map { it.bookId }.moved(from, to))
                        }
                        BookShelfRow(book.title, book.authors.joinToString(", "), book.coverImageUrl, Modifier.weight(1f)) { onBook(book.asArchive().asArgs()) }
                        if (editing) TextButton(onClick = { viewModel.remove(detail.libraryId, book.bookId) }, enabled = !state.mutating) { Text("빼기") }
                    }
                }
            }
            }
        } else Spacer(Modifier.weight(1f))
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (creating || namingId != null) {
        val current = state.shelves.firstOrNull { it.libraryId == namingId }
        ShelfNameSheet(current?.name.orEmpty(), current != null, state.mutating,
            onDismiss = { if (!state.mutating) { creating = false; namingId = null } },
            onSave = { name ->
                val done = { creating = false; namingId = null }
                if (current == null) viewModel.create(name, done) else viewModel.rename(current.libraryId, name, done)
            })
    }
    deleting?.let { shelf ->
        ShelfSheet("책장을 삭제할까요?", onDismiss = { if (!state.mutating) deleting = null }) {
            Text("${shelf.name}의 배치가 삭제돼요. 책의 별점은 유지돼요.")
            ChaekOverlayButton("삭제", { viewModel.delete(shelf.libraryId) { deleting = null } }, enabled = !state.mutating)
        }
    }
    if (adding) ShelfAddSheet(state.shelves, onDismiss = { adding = false }, onAdd = { adding = false; onAdd(it) })
    if (sharing && detail != null && token != null && !state.detailStale) BookshelfImageSheet(detail, token, onDismiss = { sharing = false })
}

@Composable
private fun ShelfMenu(shelf: Bookshelf, rename: () -> Unit, delete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.testTag("bookshelf-menu-${shelf.libraryId}")) { Text("⋯", fontFamily = ChaekIconFontFamily()) }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("이름 바꾸기") }, onClick = { expanded = false; rename() })
            if (!shelf.isDefault) DropdownMenuItem(text = { Text("책장 삭제") }, onClick = { expanded = false; delete() })
        }
    }
}

@Composable
private fun ShelfNameSheet(initial: String, rename: Boolean, saving: Boolean, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val input = rememberLimitedTextFieldState(BookshelfNameInputPolicy, initial)
    ShelfSheet(if (rename) "책장 이름 바꾸기" else "새 책장 만들기", onDismiss) {
        OutlinedTextField(input, inputTransformation = BookshelfNameInputPolicy.inputTransformation,
            lineLimits = TextFieldLineLimits.SingleLine, label = { Text("책장 이름 (1-10자)") }, modifier = Modifier.fillMaxWidth().testTag("bookshelfName"))
        ChaekOverlayButton(if (rename) "저장" else "만들기", { onSave(input.text.toString()) }, enabled = !saving)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShelfSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = ChaekSurface, shape = ChaekOverlayTokens.sheetShape) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                ChaekCloseButton(onDismiss)
            }
            content()
        }
    }
}

@Composable
internal fun ShelfChoiceSheet(viewModel: LibrarySaveViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (state.choosingBook == null) return
    ShelfSheet("담을 책장 고르기", viewModel::dismissShelfChoice) {
        Text("최근 담은 책장이 선택되어 있어요.", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(state.shelves, key = { it.libraryId }) { shelf ->
                Row(Modifier.fillMaxWidth().clickable(enabled = state.savingIsbn == null) { viewModel.selectShelf(shelf.libraryId) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(shelf.name)
                        Text("${shelf.totalCount}권", style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(state.selectedShelfId == shelf.libraryId, onClick = { viewModel.selectShelf(shelf.libraryId) }, enabled = state.savingIsbn == null)
                }
                HorizontalDivider(color = ChaekBorder)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaekOverlayButton("닫기", viewModel::dismissShelfChoice, enabled = state.savingIsbn == null, secondary = true, modifier = Modifier.weight(1f))
            ChaekOverlayButton("이 책장에 담기", viewModel::confirmShelf, enabled = state.savingIsbn == null, modifier = Modifier.weight(1f))
        }
        if (state.showLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
internal fun BookShelfRow(title: String, author: String, cover: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AsyncImage(cover, "$title 표지", Modifier.size(42.dp, 60.dp))
        Column(Modifier.weight(1f)) { Text(title); Text(author, style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary) }
    }
}

@Composable
private fun SpineRow(books: List<ArchiveBookUiModel>, onBook: (ArchiveBookUiModel) -> Unit, onMove: ((Int, Int) -> Unit)? = null) {
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    LazyRow(Modifier.fillMaxWidth().height(269.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        itemsIndexed(books, key = { _, it -> it.bookId }) { index, book ->
            LibraryBookSpine(book, Modifier.height(260.dp).clickable { onBook(book) }.then(if (onMove == null) Modifier else Modifier.pointerInput(index, books) {
                var delta = 0f
                detectDragGesturesAfterLongPress(onDragStart = { delta = 0f }, onDragEnd = {
                    val target = (index + (delta / threshold).toInt()).coerceIn(books.indices)
                    if (target != index) onMove(index, target)
                }, onDrag = { change, amount -> change.consume(); delta += amount.x })
            }))
        }
        if (books.isEmpty()) item { Text("책을 담아 책장을 채워 보세요", Modifier.padding(vertical = 24.dp), color = ChaekInkSecondary) }
    }
}

@Composable
private fun ReorderHandle(index: Int, count: Int, enabled: Boolean, tag: String, rowHeight: Float = 88f, move: (Int, Int) -> Unit) {
    val threshold = with(LocalDensity.current) { rowHeight.dp.toPx() }
    val currentMove by rememberUpdatedState(move)
    Box(Modifier.size(44.dp).testTag(tag).pointerInput(index, count, enabled) {
        var delta = 0f
        detectDragGesturesAfterLongPress(onDragStart = { delta = 0f }, onDragEnd = {
            if (enabled) { val target = (index + (delta / threshold).toInt()).coerceIn(0, count - 1); if (target != index) currentMove(index, target) }
        }, onDrag = { change, amount -> change.consume(); delta += amount.y })
    }, contentAlignment = Alignment.Center) { Text("☰", fontFamily = ChaekIconFontFamily()) }
}

internal fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }
internal fun BookshelfBookSummary.asArchive() = ArchiveBookUiModel(isbn13, bookId, title, "", "", "", "", spineImageUrl, ReadingStatus.READING, 0, 0, 0)
internal fun BookshelfBook.asArchive() = ArchiveBookUiModel(isbn13, bookId, title, authors.joinToString(", "), "", "", coverImageUrl, spineImageUrl, ReadingStatus.entries.firstOrNull { it.apiValue == status } ?: ReadingStatus.READING, currentPage, totalPages ?: 0, 0)
internal fun ArchiveBookUiModel.asArgs() = BookDetailArgs(id = id, isbn13 = id, bookId = bookId, title = title, creator = creator, coverUrl = coverUrl, totalPages = totalPages)
