package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import chaekchaek.shared.generated.resources.*
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
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            ShelfIconButton(Res.drawable.bookshelf_back, "책장 목록으로", { viewModel.close(); editing = false })
            Text("책 플레이리스트", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            selected?.let { shelf -> ShelfMenu(shelf, { namingId = shelf.libraryId }, { deleting = shelf }) }
        }
        if (!signedIn) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("나만의 책장을 만들어 보세요", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                ChaekOverlayButton("로그인", onLogin)
            }
        } else if (state.selectedId == null) {
            Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("나의 책장", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
                Box(Modifier.size(44.dp).testTag("bookshelf-create").clickable(enabled = !state.mutating, role = Role.Button) { creating = true }, contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp).border(1.dp, ChaekInk, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(Res.drawable.bookshelf_add), "새 책장 만들기", Modifier.size(18.dp), tint = ChaekInk)
                    }
                }
            }
            ChaekRefreshLayout(
                com.chaekchaek.app.presentation.common.RefreshUiState(isRefreshing = state.refreshing, enabled = !state.mutating),
                viewModel::refresh, Modifier.weight(1f),
            ) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
                itemsIndexed(state.shelves, key = { _, it -> it.libraryId }) { index, shelf ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 25.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ReorderHandle(index, state.shelves.size, canOrder, "bookshelf-reorder-${shelf.libraryId}", 345f, compact = true) { from, to ->
                                viewModel.orderShelves(state.shelves.map { it.libraryId }.moved(from, to))
                            }
                            Text(shelf.name, Modifier.weight(1f).heightIn(min = 44.dp).wrapContentHeight().testTag("bookshelf-${shelf.libraryId}").clickable(role = Role.Button) { viewModel.open(shelf.libraryId) }, color = ChaekInk, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${shelf.totalCount}권", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp), color = ChaekInkSecondary)
                            ShelfMenu(shelf, { namingId = shelf.libraryId }, { deleting = shelf })
                        }
                        Spacer(Modifier.height(7.dp))
                        SpineRow(shelf.items.map { it.asArchive() }, onBook = { viewModel.open(shelf.libraryId) }, onMove = if (!canOrder) null else { from, to ->
                            viewModel.orderBooks(shelf.libraryId, shelf.items.map { it.bookId }.moved(from, to))
                        })
                        HorizontalDivider(thickness = 3.dp, color = ChaekBorder)
                    }
                }
                item { CreateShelfButton({ creating = true }, !state.mutating, Modifier.padding(horizontal = 24.dp)) }
            }
            }
        } else if (detail != null) {
            ChaekRefreshLayout(
                com.chaekchaek.app.presentation.common.RefreshUiState(isRefreshing = state.refreshing || state.detailRefreshing, enabled = !state.mutating && !editing),
                viewModel::refresh, Modifier.weight(1f),
            ) {
            LazyColumn(Modifier.fillMaxSize(), state = detailListState, contentPadding = PaddingValues(24.dp)) {
                item {
                    Text(detail.name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
                    Spacer(Modifier.height(8.dp))
                    Text("${detail.totalCount}권", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
                }
                item { Spacer(Modifier.height(16.dp)); SpineRow(detail.items.map { it.asArchive() }, onBook = { onBook(it.asArgs()) }, onMove = if (!canOrder) null else { from, to ->
                    viewModel.orderBooks(detail.libraryId, detail.items.map { it.bookId }.moved(from, to))
                }); HorizontalDivider(thickness = 3.dp, color = ChaekBorder) }
                item {
                    Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChaekOverlayButton("+ 책 담기", { adding = true }, modifier = Modifier.weight(1f), enabled = !state.mutating)
                        ShelfIconButton(Res.drawable.bookshelf_share, "이미지 공유", { sharing = true }, enabled = !state.mutating && !state.refreshing && !state.detailRefreshing && !state.detailStale, outlined = true)
                        ShelfIconButton(Res.drawable.bookshelf_download, "이미지 저장", { sharing = true }, enabled = !state.mutating && !state.refreshing && !state.detailRefreshing && !state.detailStale, outlined = true)
                    }
                }
                item { Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("이 책장에 담긴 책", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { editing = !editing }) { Text(if (editing) "완료" else "편집") }
                } }
                itemsIndexed(detail.items, key = { _, it -> it.bookId }) { index, book ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BookShelfRow(book.title, book.authors.joinToString(", "), book.coverImageUrl, Modifier.weight(1f)) { onBook(book.asArchive().asArgs()) }
                        if (!editing) ReorderHandle(index, detail.items.size, canOrder, "bookshelf-book-reorder-${book.bookId}") { from, to ->
                            viewModel.orderBooks(detail.libraryId, detail.items.map { it.bookId }.moved(from, to))
                        }
                        if (editing) TextButton(onClick = { viewModel.remove(detail.libraryId, book.bookId) }, enabled = !state.mutating) { Text("빼기") }
                    }
                    HorizontalDivider(color = ChaekBorder)
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
        ShelfIconButton(Res.drawable.bookshelf_more, "${shelf.name} 더보기", { expanded = true }, modifier = Modifier.testTag("bookshelf-menu-${shelf.libraryId}"))
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = ChaekSurface, shape = ChaekOverlayTokens.sheetShape, dragHandle = {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 9.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(36.dp, 4.dp).background(Color(0xFFDCDCE0), CircleShape))
        }
    }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium.copy(lineHeight = 29.sp))
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
    if (state.creatingShelf) {
        ShelfNameSheet("", false, state.savingIsbn != null, viewModel::cancelShelfCreation, viewModel::createShelf)
        return
    }
    ShelfSheet("담을 책장 고르기", viewModel::dismissShelfChoice) {
        Text("최근 담은 책장이 선택되어 있어요.", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp), color = ChaekInkSecondary)
        LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(state.shelves, key = { it.libraryId }) { shelf ->
                Row(Modifier.fillMaxWidth().clickable(enabled = state.savingIsbn == null) { viewModel.selectShelf(shelf.libraryId) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ShelfThumbnail(shelf)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(shelf.name, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
                        Text("${shelf.totalCount}권", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp))
                    }
                    val chosen = state.selectedShelfId == shelf.libraryId
                    Box(Modifier.size(44.dp).semantics { selected = chosen }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(22.dp).background(if (chosen) ChaekInk else Color.Transparent, CircleShape).border(1.dp, if (chosen) ChaekInk else ChaekInkSecondary.copy(alpha = .5f), CircleShape), contentAlignment = Alignment.Center) {
                            if (chosen) Text("✓", fontFamily = ChaekIconFontFamily(), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                HorizontalDivider(color = ChaekBorder)
            }
        }
        CreateShelfButton(viewModel::beginShelfCreation, state.savingIsbn == null, Modifier.testTag("shelf-choice-create"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChaekOverlayButton("닫기", viewModel::dismissShelfChoice, enabled = state.savingIsbn == null, secondary = true, modifier = Modifier.weight(1f))
            ChaekOverlayButton("이 책장에 담기", viewModel::confirmShelf, enabled = state.savingIsbn == null, modifier = Modifier.weight(1f))
        }
        if (state.showLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@Composable
internal fun BookShelfRow(title: String, author: String, cover: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clickable(onClick = onClick).heightIn(min = 84.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AsyncImage(cover, "$title 표지", Modifier.size(40.dp, 58.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp)); Text(author, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp), color = ChaekInkSecondary) }
    }
}

@Composable
private fun SpineRow(books: List<ArchiveBookUiModel>, onBook: (ArchiveBookUiModel) -> Unit, onMove: ((Int, Int) -> Unit)? = null) {
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    LazyRow(Modifier.fillMaxWidth().height(269.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        itemsIndexed(books, key = { _, it -> it.bookId }) { index, book ->
            LibraryBookSpine(book, Modifier.height(bookshelfSpineHeight(index)).widthIn(min = 6.dp, max = 48.dp).clickable { onBook(book) }.then(if (onMove == null) Modifier else Modifier.pointerInput(index, books) {
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
private fun ReorderHandle(index: Int, count: Int, enabled: Boolean, tag: String, rowHeight: Float = 88f, compact: Boolean = false, move: (Int, Int) -> Unit) {
    val threshold = with(LocalDensity.current) { rowHeight.dp.toPx() }
    val currentMove by rememberUpdatedState(move)
    Box(Modifier.width(if (compact) 32.dp else 44.dp).height(44.dp).testTag(tag).semantics { contentDescription = "길게 눌러 순서 변경" }.pointerInput(index, count, enabled) {
        var delta = 0f
        detectDragGesturesAfterLongPress(onDragStart = { delta = 0f }, onDragEnd = {
            if (enabled) { val target = (index + (delta / threshold).toInt()).coerceIn(0, count - 1); if (target != index) currentMove(index, target) }
        }, onDrag = { change, amount -> change.consume(); delta += amount.y })
    }, contentAlignment = if (compact) Alignment.CenterStart else Alignment.Center) { Icon(painterResource(Res.drawable.bookshelf_grip), null, Modifier.size(20.dp, 24.dp), tint = Color(0xFF777777)) }
}

@Composable
private fun ShelfIconButton(icon: DrawableResource, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, outlined: Boolean = false) {
    Box(modifier.size(if (outlined) 56.dp else 44.dp, if (outlined) 48.dp else 44.dp)
        .then(if (outlined) Modifier.border(1.dp, ChaekBorder, RoundedCornerShape(16.dp)) else Modifier)
        .clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), label, Modifier.size(22.dp), tint = ChaekInk.copy(alpha = if (enabled) 1f else .4f))
    }
}

@Composable
private fun CreateShelfButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val border = ChaekInkSecondary.copy(alpha = .5f)
    Row(modifier.fillMaxWidth().height(52.dp).drawBehind {
        drawRoundRect(border, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
    }.clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(painterResource(Res.drawable.bookshelf_plus), null, Modifier.size(22.dp), tint = ChaekInk)
        Spacer(Modifier.width(8.dp))
        Text("새 책장 만들기", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
    }
}

@Composable
private fun ShelfThumbnail(shelf: Bookshelf) {
    Column(Modifier.size(65.dp, 48.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.Bottom) {
            shelf.items.take(5).forEach { book ->
                LibraryBookSpine(book.asArchive(), Modifier.height(44.dp).widthIn(min = 3.dp, max = 12.dp))
            }
        }
        HorizontalDivider(thickness = 2.dp, color = ChaekBorder)
    }
}

// 승인 A HTML의 책등 높이와 140% 배율을 유지한다. 표지 대체와 실제 책등 모두 같은 배치를 사용한다.
private fun bookshelfSpineHeight(index: Int): Dp = listOf(156.928f, 177f, 144.155f, 135.031f, 157.84f, 135.031f, 159.665f, 173.351f, 148.716f, 133.206f, 149.629f, 166.964f, 125.907f, 169.701f)[index % 14].times(1.4f).dp

internal fun <T> List<T>.moved(from: Int, to: Int): List<T> = toMutableList().apply { add(to, removeAt(from)) }
internal fun BookshelfBookSummary.asArchive() = ArchiveBookUiModel(isbn13, bookId, title, "", "", "", "", spineImageUrl, ReadingStatus.READING, 0, 0, 0)
internal fun BookshelfBook.asArchive() = ArchiveBookUiModel(isbn13, bookId, title, authors.joinToString(", "), "", "", coverImageUrl, spineImageUrl, ReadingStatus.entries.firstOrNull { it.apiValue == status } ?: ReadingStatus.READING, currentPage, totalPages ?: 0, 0)
internal fun ArchiveBookUiModel.asArgs() = BookDetailArgs(id = id, isbn13 = id, bookId = bookId, title = title, creator = creator, coverUrl = coverUrl, totalPages = totalPages)
