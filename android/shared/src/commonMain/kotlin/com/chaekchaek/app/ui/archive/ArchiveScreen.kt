package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaekchaek.app.domain.shelf.ReadingStatus
import kotlinx.coroutines.launch

@Composable
fun ArchiveRoute(
    viewModel: ArchiveViewModel,
    accessToken: String?,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onBookClick: (ArchiveBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    bookCover: @Composable (ArchiveBookUiModel) -> Unit = { DefaultBookCover(it) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(accessToken) { viewModel.authenticate(accessToken) }
    ArchiveScreen(
        uiState = uiState,
        editing = editing,
        onEditingChange = onEditingChange,
        onRemove = viewModel::remove,
        onChangeStatus = viewModel::changeStatus,
        onAnonymousReviewsChange = viewModel::setAnonymousReviews,
        onRetry = viewModel::retry,
        onBookClick = onBookClick,
        modifier = modifier,
        bookCover = bookCover,
    )
}

@Composable
fun ArchiveScreen(
    uiState: ArchiveUiState,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onRemove: (Set<String>) -> Unit,
    onChangeStatus: (Set<String>, ReadingStatus) -> Unit,
    onAnonymousReviewsChange: (Boolean, String) -> Unit,
    onRetry: () -> Unit,
    onBookClick: (ArchiveBookUiModel) -> Unit,
    modifier: Modifier = Modifier,
    bookCover: @Composable (ArchiveBookUiModel) -> Unit = { DefaultBookCover(it) },
) {
    var filter by rememberSaveable { mutableStateOf<ReadingStatus?>(null) }
    var sort by rememberSaveable { mutableStateOf(ArchiveSort.Recent) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingDeletionIds by remember { mutableStateOf(emptySet<String>()) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nickname by rememberSaveable(uiState.nickname) { mutableStateOf(uiState.nickname) }
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

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                    AnonymousSetting(
                        checked = uiState.anonymousReviews,
                        onClick = {
                            if (uiState.anonymousReviews) showNicknameDialog = true
                            else onAnonymousReviewsChange(true, "")
                        },
                    )
                } else {
                    LibraryTopBar(onEdit = { onEditingChange(true) })
                }
            }
            item {
                StatusFilters(selected = filter, onSelected = { filter = it })
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

        if (uiState.showLoading) {
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
    if (showNicknameDialog) {
        NicknameDialog(
            nickname = nickname,
            onNicknameChange = { nickname = it.take(10) },
            onDismiss = { showNicknameDialog = false },
            onConfirm = {
                onAnonymousReviewsChange(false, nickname)
                showNicknameDialog = false
            },
        )
    }
}

@Composable
private fun LibraryTopBar(onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("내 서재", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onEdit) {
            Text("편집", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
        ProfileAvatar()
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
        Text("${selectedCount}권 선택", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDone) {
            Text("완료", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
        }
        ProfileAvatar()
    }
}

@Composable
private fun ProfileAvatar() {
    Surface(
        modifier = Modifier.size(28.dp).semantics { contentDescription = "프로필" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) { Text("🐦", modifier = Modifier.clearAndSetSemantics {}, fontSize = 13.sp) }
    }
}

@Composable
private fun AnonymousSetting(checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionBox(selected = checked)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("익명으로 감상 공개", style = MaterialTheme.typography.titleSmall)
            Text(
                if (checked) "해제하면 닉네임을 설정해야 합니다" else "닉네임이 감상에 표시됩니다",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatusFilters(selected: ReadingStatus?, onSelected: (ReadingStatus?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusFilterChip("전체", selected == null) { onSelected(null) }
        ReadingStatus.entries.forEach { status ->
            StatusFilterChip(status.label, selected == status) { onSelected(status) }
        }
    }
}

@Composable
private fun StatusFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline),
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall)
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
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(countLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.weight(1f))
        Box {
            Surface(onClick = { expanded = true }, color = Color.Transparent) {
                Text("${sort.label}⌄", modifier = Modifier.padding(4.dp), style = MaterialTheme.typography.labelMedium)
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editing) SelectionBox(selected)
        Surface(
            modifier = Modifier.size(width = 56.dp, height = 80.dp).shadow(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Box(contentAlignment = Alignment.Center) { bookCover(book) }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ReadingStatusTag(book.status)
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(book.creator, book.category.ifBlank { book.publisher }).filter(String::isNotBlank).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (book.totalPages > 0) "${book.currentPage}쪽 / ${book.totalPages}쪽" else "쪽수 정보 없음",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Box(modifier = Modifier.width(116.dp).height(2.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(
                    modifier = Modifier.fillMaxWidth(book.progressRatio.coerceIn(0f, 1f)).height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface),
                )
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
                Text("⌫", modifier = Modifier.clearAndSetSemantics {}, color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
            }
        } else {
            Text("›", modifier = Modifier.clearAndSetSemantics {}, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp)
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
        if (selected) Text("✓", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp)
    }
}

@Composable
private fun ReadingStatusTag(status: ReadingStatus) {
    val selected = status == ReadingStatus.READING
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline),
    ) {
        Text(
            status.label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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
            Text("⌃", style = MaterialTheme.typography.titleSmall)
            Text(
                "TOP",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
    ArchiveDialog(onDismiss = onDismiss) {
        Text("독서 상태 변경", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
        Text(
            "선택한 ${selectedCount}권의 상태를 변경합니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
        )
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReadingStatus.entries.forEach { status ->
                StatusOptionRow(status = status, selected = selected == status, onClick = { selected = status })
            }
        }
        DialogActions(confirmLabel = "변경", onDismiss = onDismiss, onConfirm = { onChange(selected) })
    }
}

@Composable
private fun DeleteConfirmationDialog(selectedCount: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    ArchiveDialog(onDismiss = onDismiss) {
        Text("책 삭제", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
        Text(
            "선택한 ${selectedCount}권을 서재에서 삭제할까요?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
        )
        DialogActions(confirmLabel = "삭제", onDismiss = onDismiss, onConfirm = onConfirm)
    }
}

@Composable
private fun ArchiveDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = 80.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
            }
        }
    }
}

@Composable
private fun DialogActions(
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) { Text("취소", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)) }
        Button(
            onClick = onConfirm,
            modifier = Modifier.weight(1f).height(48.dp),
            enabled = confirmEnabled,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.outline,
                disabledContentColor = MaterialTheme.colorScheme.surface,
            ),
        ) { Text(confirmLabel, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)) }
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
private fun NicknameDialog(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ArchiveDialog(onDismiss = onDismiss) {
        Text("닉네임 설정", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp))
        Text(
            "기록과 감상에 표시할 닉네임이 필요해요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
        )
        NicknameInput(nickname = nickname, onNicknameChange = onNicknameChange)
        DialogActions(
            confirmLabel = "확인",
            confirmEnabled = nickname.isNotBlank(),
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun NicknameInput(nickname: String, onNicknameChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.5.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box {
                    if (nickname.isEmpty()) {
                        Text(
                            "닉네임을 입력하세요",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
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
