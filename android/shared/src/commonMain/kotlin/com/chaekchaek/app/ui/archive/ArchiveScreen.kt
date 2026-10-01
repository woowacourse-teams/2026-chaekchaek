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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.domain.shelf.ReadingStatus
import com.chaekchaek.app.domain.reader.Nickname
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
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
) {
    var filter by rememberSaveable { mutableStateOf<ReadingStatus?>(null) }
    var sort by rememberSaveable { mutableStateOf(ArchiveSort.Recent) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingDeletionIds by remember { mutableStateOf(emptySet<String>()) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val visibleItems = remember(uiState.items, filter, sort) {
        sortArchiveBooks(uiState.items.filter { filter == null || it.status == filter }, sort)
    }
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
            contentPadding = PaddingValues(bottom = if (editing) 88.dp else 12.dp),
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
                } else {
                    LibraryTopBar(
                        displayName = memberSettingsState.publicNickname,
                        onProfileClick = onProfileClick,
                        onEdit = { onEditingChange(true) },
                    )
                }
            }
            item {
                LibraryControls(
                    selected = filter,
                    onSelected = { filter = it },
                )
                SortRow(
                    countLabel = "${filter?.label ?: "전체"} ${visibleItems.size}권",
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = if (editing) 88.dp else 16.dp),
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
private fun LibraryTopBar(displayName: String, onProfileClick: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "내 서재",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, fontSize = 29.sp),
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onEdit) {
            Text(
                "편집",
                color = Color(0xFF555555),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            )
        }
    }
}

@Composable
private fun EditTopBar(selectedCount: Int, onCancel: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 0.dp)) {
            Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Text("${selectedCount}권 선택", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDone) {
            Text("완료", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun ProfileButton(displayName: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(onClickLabel = "마이페이지 열기", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "프로필" },
        contentAlignment = Alignment.Center,
    ) {
        MemberAvatar(displayName, 32.dp)
    }
}

@Composable
private fun LibraryControls(
    selected: ReadingStatus?,
    onSelected: (ReadingStatus?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusFilterChip("전체", selected == null) { onSelected(null) }
            listOf(ReadingStatus.READING, ReadingStatus.FINISHED, ReadingStatus.WANT_TO_READ).forEach { status ->
                StatusFilterChip(status.label, selected == status) { onSelected(status) }
            }
        }
    }
}

@Composable
private fun StatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        border = null,
    ) {
        Box(
            modifier = Modifier.background(if (selected) Color.Transparent else Color(0xFFF3F3F5)).padding(horizontal = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
        }
    }
}

@Composable
private fun SortRow(
    countLabel: String,
    sort: ArchiveSort,
    onSortChange: (ArchiveSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            countLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        )
        Spacer(Modifier.weight(1f))
        Box {
            Surface(onClick = { expanded = true }, color = Color.Transparent) {
                Text(
                    "${sort.label}⌄",
                    modifier = Modifier.padding(4.dp),
                    color = Color(0xFF555555),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ArchiveSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
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
    Box(modifier = Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onRetry) { Text("다시 시도") }
        }
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
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onStatusChange,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(4.dp),
            ) { Text("상태 변경") }
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(48.dp),
                enabled = enabled,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) { Text("서재에서 삭제") }
        }
    }
}

@Composable
private fun StatusChangeDialog(selectedCount: Int, onDismiss: () -> Unit, onChange: (ReadingStatus) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(ReadingStatus.READING) }
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("독서 상태 변경", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "선택한 ${selectedCount}권의 상태를 변경합니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReadingStatus.entries.forEach { status ->
                        StatusOptionRow(status = status, selected = selected == status, onClick = { selected = status })
                    }
                }
            }
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = { DialogConfirmButton(label = "변경", onClick = { onChange(selected) }) },
    )
}

@Composable
private fun DeleteConfirmationDialog(selectedCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("책 삭제", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "선택한 ${selectedCount}권을 서재에서 삭제할까요? 삭제한 책은 다시 복구할 수 없어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = { DialogConfirmButton(label = "삭제", onClick = onConfirm, destructive = true) },
    )
}

@Composable
private fun DialogDismissButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(6.dp)) {
        Text("취소", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DialogConfirmButton(label: String, onClick: () -> Unit, enabled: Boolean = true, destructive: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatusOptionRow(status: ReadingStatus, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(20.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
        }
        Text(
            status.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
internal fun NicknameDialog(
    nicknameState: TextFieldState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val nickname = nicknameState.text.toString()
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("닉네임 설정", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "기록과 감상에 표시할 닉네임이 필요해요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
                NicknameInput(nicknameState)
                Text("공백이 아닌 최대 10자", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { DialogDismissButton(onDismiss) },
        confirmButton = {
            DialogConfirmButton(
                label = "확인",
                onClick = onConfirm,
                enabled = Nickname.isValid(nickname.trim()),
            )
        },
    )
}

@Composable
private fun NicknameInput(nicknameState: TextFieldState) {
    val nickname = nicknameState.text.toString()
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            state = nicknameState,
            modifier = Modifier.weight(1f),
            inputTransformation = InputTransformation.maxLength(Nickname.MAX_LENGTH),
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorator = { innerTextField ->
                Box {
                    if (nickname.isEmpty()) {
                        Text(
                            "닉네임을 입력하세요",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        )
                    }
                    innerTextField()
                }
            },
        )
        Text(
            "${nickname.length}/10",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        )
    }
}
