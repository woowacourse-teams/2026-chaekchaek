package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.common.ChaekDropdown
import com.chaekchaek.app.ui.common.ChaekFilterChip
import com.chaekchaek.app.ui.common.BrandHeader
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.libraryBookShadow
import kotlinx.coroutines.launch

@Composable
fun ArchiveRoute(
    viewModel: ArchiveViewModel,
    memberSettingsViewModel: MemberSettingsViewModel,
    editing: Boolean,
    scrollTopRequest: Int = 0,
    onEditingChange: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onBookClick: (ArchiveBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    bookCover: @Composable (ArchiveBookUiModel) -> Unit = { DefaultBookCover(it) },
    bookSpine: @Composable (ArchiveBookUiModel, Modifier, Boolean, (String) -> Unit) -> Unit =
        { _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    ArchiveScreen(
        uiState = uiState,
        memberSettingsState = memberSettingsState,
        editing = editing,
        scrollTopRequest = scrollTopRequest,
        onEditingChange = onEditingChange,
        onRemove = viewModel::remove,
        onChangeStatus = viewModel::changeStatus,
        onRetry = viewModel::retry,
        onProfileClick = onProfileClick,
        onBookClick = onBookClick,
        modifier = modifier,
        bookCover = bookCover,
        bookSpine = bookSpine,
    )
}

@Composable
fun ArchiveScreen(
    uiState: ArchiveUiState,
    memberSettingsState: MemberSettingsUiState,
    editing: Boolean,
    scrollTopRequest: Int = 0,
    onEditingChange: (Boolean) -> Unit,
    onRemove: (Set<String>) -> Unit,
    onChangeStatus: (Set<String>, ReadingStatus) -> Unit,
    onRetry: () -> Unit,
    onProfileClick: () -> Unit,
    onBookClick: (ArchiveBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    bookCover: @Composable (ArchiveBookUiModel) -> Unit = { DefaultBookCover(it) },
    bookSpine: @Composable (ArchiveBookUiModel, Modifier, Boolean, (String) -> Unit) -> Unit =
        { _, _, _, _ -> },
) {
    var filter by rememberSaveable { mutableStateOf<ReadingStatus?>(null) }
    var sort by rememberSaveable { mutableStateOf(ArchiveSort.Recent) }
    var onlySpineImages by rememberSaveable { mutableStateOf(false) }
    var failedSpineUrls by remember { mutableStateOf(emptySet<String>()) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingDeletionIds by remember { mutableStateOf(emptySet<String>()) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val visibleItems = remember(uiState.items, filter, sort) {
        sortArchiveBooks(uiState.items.filter { filter == null || it.status == filter }, sort)
    }
    val spineBooks = booksForSpineShelf(visibleItems, onlySpineImages, failedSpineUrls)
    val density = LocalDensity.current
    val scrollTopThresholdPx = remember(density) { with(density) { 240.dp.roundToPx() } }
    val showScrollTop by remember(scrollTopThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset >= scrollTopThresholdPx)
        }
    }

    LaunchedEffect(uiState.items) {
        selectedIds = selectedIds.intersect(uiState.items.mapTo(mutableSetOf()) { it.id })
    }
    LaunchedEffect(scrollTopRequest) {
        if (scrollTopRequest > 0) listState.animateScrollToItem(0)
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = if (editing) 0.dp else 70.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = if (editing) 188.dp else 12.dp),
        ) {
            item {
                if (editing) {
                    EditTopBar(
                        selectedCount = selectedIds.size,
                        onCancel = {
                            selectedIds = emptySet()
                            onEditingChange(false)
                        },
                        onDone = {
                            selectedIds = emptySet()
                            onEditingChange(false)
                        },
                    )
                }
            }
            item {
                LibraryControls(
                    selected = filter,
                    onSelected = { filter = it },
                    onEdit = if (editing) null else ({ onEditingChange(true) }),
                )
                if (!editing && visibleItems.isNotEmpty()) {
                    BookSpineShelf(
                        books = spineBooks,
                        onlySpineImages = onlySpineImages,
                        onOnlySpineImagesChange = { onlySpineImages = it },
                        onSpineImageLoadFailed = { failedUrl -> failedSpineUrls += failedUrl },
                        bookSpine = bookSpine,
                    )
                }
                SortRow(
                    countLabel = "${filter?.label ?: "전체"} ${visibleItems.size}",
                    sort = sort,
                    onSortChange = { sort = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            if (visibleItems.isEmpty()) {
                item {
                    if (uiState.errorMessage == null) EmptyLibrary()
                    else ArchiveError(uiState.errorMessage, onRetry)
                }
            } else {
                items(visibleItems, key = { it.id }) { book ->
                    LibraryBookRow(
                        book = book,
                        editing = editing,
                        selected = book.id in selectedIds,
                        onSelect = {
                            selectedIds = if (book.id in selectedIds) selectedIds - book.id else selectedIds + book.id
                        },
                        onDelete = { pendingDeletionIds = setOf(book.id) },
                        onOpen = { onBookClick(book) },
                        bookCover = bookCover,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        if (!editing) {
            BrandHeader(onProfileClick)
        }

        if (showScrollTop) {
            ScrollTopButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (editing) 188.dp else 16.dp),
            )
        }

        if (editing) {
            EditActionBar(
                enabled = selectedIds.isNotEmpty(),
                onStatusChange = { showStatusDialog = true },
                onDelete = { pendingDeletionIds = selectedIds },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (uiState.showLoading || memberSettingsState.showLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(32.dp).semantics { contentDescription = "서재를 불러오는 중" },
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        }
    }

    if (showStatusDialog) {
        StatusChangeDialog(
            selectedCount = selectedIds.size,
            onDismiss = { showStatusDialog = false },
            onChange = { status ->
                onChangeStatus(selectedIds, status)
                selectedIds = emptySet()
                showStatusDialog = false
            },
        )
    }
    if (pendingDeletionIds.isNotEmpty()) {
        DeleteConfirmationDialog(
            selectedCount = pendingDeletionIds.size,
            onDismiss = { pendingDeletionIds = emptySet() },
            onConfirm = {
                onRemove(pendingDeletionIds)
                selectedIds -= pendingDeletionIds
                pendingDeletionIds = emptySet()
            },
        )
    }
}

@Composable
private fun EditTopBar(selectedCount: Int, onCancel: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${selectedCount}권 선택", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            ChaekCloseButton(onCancel)
        }
        ChaekOverlayButton("완료", onDone, secondary = true)
    }
}

@Composable
private fun LibraryControls(
    selected: ReadingStatus?,
    onSelected: (ReadingStatus?) -> Unit,
    onEdit: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChaekFilterChip("전체", selected == null, onClick = { onSelected(null) })
            listOf(ReadingStatus.READING, ReadingStatus.FINISHED, ReadingStatus.WANT_TO_READ).forEach { status ->
                ChaekFilterChip(status.label, selected == status, onClick = { onSelected(status) })
            }
        }
        if (onEdit != null) {
            Box(
                modifier = Modifier.size(44.dp).clickable(
                    onClickLabel = "서재 편집",
                    role = Role.Button,
                    onClick = onEdit,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "편집",
                    color = Color(0xFF555555),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                )
            }
        }
    }
}

@Composable
private fun BookSpineShelf(
    books: List<ArchiveBookUiModel>,
    onlySpineImages: Boolean,
    onOnlySpineImagesChange: (Boolean) -> Unit,
    onSpineImageLoadFailed: (String) -> Unit,
    bookSpine: @Composable (ArchiveBookUiModel, Modifier, Boolean, (String) -> Unit) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 27.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(194.dp)
                .semantics { contentDescription = "책등 책장 ${books.size}권" },
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                bookSpine(
                    book,
                    Modifier.height(spineHeight(index)).widthIn(min = 6.dp, max = 34.dp),
                    !onlySpineImages,
                    onSpineImageLoadFailed,
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(4.dp)
                .background(Color(0xFFE5E3E0)),
        )
        Row(
            modifier = Modifier.align(Alignment.End).padding(end = 24.dp)
                .toggleable(
                    value = onlySpineImages,
                    role = Role.Checkbox,
                    onValueChange = onOnlySpineImagesChange,
                )
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = onlySpineImages,
                onCheckedChange = null,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = .7f; scaleY = .7f },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "책등 이미지 있는 책만",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SpineHeightPattern = listOf(
    172.dp,
    194.dp,
    158.dp,
    148.dp,
    173.dp,
    148.dp,
    175.dp,
    190.dp,
    163.dp,
    146.dp,
    164.dp,
    183.dp,
    138.dp,
    186.dp,
)

private fun spineHeight(index: Int): Dp = SpineHeightPattern[index % SpineHeightPattern.size]

@Composable
private fun SortRow(
    countLabel: String,
    sort: ArchiveSort,
    onSortChange: (ArchiveSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 26.dp, end = 24.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            countLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        )
        Spacer(Modifier.weight(1f))
        ChaekDropdown(
            selectedOption = sort,
            options = ArchiveSort.entries,
            optionLabel = ArchiveSort::label,
            onOptionSelected = onSortChange,
            contentDescription = "서재 정렬",
        )
    }
}

internal enum class ArchiveSort(val label: String) {
    Recent("최근 기록순"),
    Oldest("오래된 기록순"),
}

internal fun sortArchiveBooks(items: List<ArchiveBookUiModel>, sort: ArchiveSort): List<ArchiveBookUiModel> =
    when (sort) {
        ArchiveSort.Recent -> items.sortedByDescending { it.lastRecordedAt }
        ArchiveSort.Oldest -> items.sortedBy { it.lastRecordedAt }
    }

@Composable
private fun LibraryBookRow(
    book: ArchiveBookUiModel,
    editing: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    bookCover: @Composable (ArchiveBookUiModel) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = if (editing) onSelect else onOpen)
            .padding(horizontal = 24.dp, vertical = 21.dp),
        horizontalArrangement = Arrangement.spacedBy(17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editing) SelectionBox(selected)
        Box(
            modifier = Modifier.size(width = 67.dp, height = 96.dp).libraryBookShadow().clip(RoundedCornerShape(2.dp)),
        ) {
            Box(contentAlignment = Alignment.Center) { bookCover(book) }
        }
        Column(modifier = Modifier.weight(1f)) {
            ReadingStatusTag(book.status)
            Text(
                book.title,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(book.creator, book.category.ifBlank { book.publisher })
                    .filter(String::isNotBlank)
                    .forEachIndexed { index, metadata ->
                        Text(
                            metadata,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (index == 0) 1f else 0.7f),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }
            Row(Modifier.fillMaxWidth().padding(top = 13.dp)) {
                Text(
                    if (book.totalPages > 0) "${book.currentPage} / ${book.totalPages}쪽" else "쪽수 정보 없음",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(book.progressRatio.coerceIn(0f, 1f) * 100).toInt()}%",
                    color = Color(0xFF555555),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                )
            }
            Box(modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .background(Color(0xFFECECEE), RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(book.progressRatio.coerceIn(0f, 1f)).height(3.dp)
                            .background(
                                if (book.status == ReadingStatus.READING) ChaekAccent else Color(0xFFB1B1B7),
                                RoundedCornerShape(4.dp),
                            ),
                    )
                }
            }
        }
        if (editing) {
            Box(
                modifier = Modifier.size(48.dp).clickable(
                    onClickLabel = "서재에서 삭제",
                    role = Role.Button,
                    onClick = onDelete,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "⌫",
                    modifier = Modifier.clearAndSetSemantics {},
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 20.sp,
                    fontFamily = ChaekIconFontFamily(),
                )
            }
        } else {
            Text(
                "›",
                modifier = Modifier.clearAndSetSemantics {},
                color = Color(0xFFAAAAAA),
                fontSize = 22.sp,
                fontFamily = ChaekIconFontFamily(),
            )
        }
    }
}

@Composable
private fun DefaultBookCover(book: ArchiveBookUiModel) {
    Text(
        "책",
        modifier = Modifier.semantics { contentDescription = "${book.title} 표지" },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SelectionBox(selected: Boolean) {
    Box(
        modifier = Modifier.size(20.dp)
            .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Text(
            "✓",
            color = MaterialTheme.colorScheme.surface,
            fontSize = 12.sp,
            fontFamily = ChaekIconFontFamily(),
        )
    }
}

@Composable
private fun ReadingStatusTag(status: ReadingStatus) {
    if (status == ReadingStatus.READING) {
        Row(
            modifier = Modifier.height(20.dp).background(Color(0xFF242424), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(5.dp).background(Color(0xFFFF9500), RoundedCornerShape(2.5.dp)))
            Text(
                status.label,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
            )
        }
    } else {
        Text(
            status.label,
            modifier = Modifier.height(20.dp),
            color = Color(0xFF777777),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
        )
    }
}

@Composable
private fun EmptyLibrary() {
    Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("아직 서재가 비어 있어요", style = MaterialTheme.typography.headlineSmall)
            Text(
                "발견에서 읽고 싶은 책을 등록해 보세요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ArchiveError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("서재를 불러오지 못했어요", style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        ChaekOverlayButton("다시 시도", onRetry)
    }
}

@Composable
private fun ScrollTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp).shadow(6.dp, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "⌃",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = ChaekIconFontFamily(),
            )
            Text(
                "TOP",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            )
        }
    }
}

@Composable
private fun EditActionBar(
    enabled: Boolean,
    onStatusChange: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChaekOverlayButton("상태 변경", onStatusChange, enabled = enabled, secondary = true)
            ChaekOverlayButton("서재에서 삭제", onDelete, enabled = enabled)
        }
    }
}
