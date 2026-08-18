package com.chamsae.chaekchaek.ui.archive

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.chamsae.chaekchaek.data.ArchiveRepository
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.ReadingStatus
import kotlinx.coroutines.launch

@Composable
fun ArchiveScreen(
  archiveRepository: ArchiveRepository,
  editing: Boolean,
  onEditingChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val items by archiveRepository.items.collectAsState()
  val anonymousReviews by archiveRepository.anonymousReviews.collectAsState()
  var filter by rememberSaveable { mutableStateOf<ReadingStatus?>(null) }
  var selectedIds by remember { mutableStateOf(emptySet<String>()) }
  var showStatusDialog by remember { mutableStateOf(false) }
  var showNicknameDialog by remember { mutableStateOf(false) }
  var nickname by rememberSaveable { mutableStateOf(archiveRepository.nickname()) }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val visibleItems = remember(items, filter) {
    items.filter { filter == null || it.status == filter }.sortedByDescending { it.lastRecordedAt }
  }
  val showScrollTop by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset >= 240 }
  }

  LaunchedEffect(items) {
    selectedIds = selectedIds.intersect(items.mapTo(mutableSetOf()) { it.id })
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
            checked = anonymousReviews,
            onClick = {
              if (anonymousReviews) showNicknameDialog = true
              else archiveRepository.setAnonymousReviews(true)
            },
          )
        } else {
          LibraryTopBar()
        }
      }
      item {
        StatusFilters(selected = filter, onSelected = { filter = it })
        SortRow(
          countLabel = "${filter?.label ?: "전체"} ${visibleItems.size}권",
          editing = editing,
          onEdit = { onEditingChange(true) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
      }
      if (visibleItems.isEmpty()) {
        item { EmptyLibrary() }
      } else {
        items(visibleItems, key = { it.id }) { book ->
          LibraryBookRow(
            book = book,
            editing = editing,
            selected = book.id in selectedIds,
            onSelect = {
              selectedIds =
                if (book.id in selectedIds) selectedIds - book.id
                else selectedIds + book.id
            },
            onDelete = {
              selectedIds -= book.id
              archiveRepository.remove(setOf(book.id))
            },
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
      }
    }

    if (showScrollTop) {
      ScrollTopButton(
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = if (editing) 88.dp else 16.dp),
      )
    }

    if (editing) {
      EditActionBar(
        enabled = selectedIds.isNotEmpty(),
        onStatusChange = { showStatusDialog = true },
        onDelete = {
          archiveRepository.remove(selectedIds)
          selectedIds = emptySet()
        },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }

  if (showStatusDialog) {
    StatusChangeDialog(
      selectedCount = selectedIds.size,
      onDismiss = { showStatusDialog = false },
      onChange = { status ->
        archiveRepository.changeStatus(selectedIds, status)
        selectedIds = emptySet()
        showStatusDialog = false
      },
    )
  }

  if (showNicknameDialog) {
    NicknameDialog(
      nickname = nickname,
      onNicknameChange = { nickname = it.take(10) },
      onDismiss = { showNicknameDialog = false },
      onConfirm = {
        archiveRepository.setAnonymousReviews(false, nickname)
        showNicknameDialog = false
      },
    )
  }
}

@Composable
private fun LibraryTopBar() {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text("내 서재", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
    Spacer(Modifier.weight(1f))
    ProfileAvatar()
  }
}

@Composable
private fun EditTopBar(
  selectedCount: Int,
  onCancel: () -> Unit,
  onDone: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TextButton(onClick = onCancel, contentPadding = PaddingValues(horizontal = 0.dp)) {
      Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    Text("${selectedCount}권 선택", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp))
    Spacer(Modifier.weight(1f))
    TextButton(onClick = onDone) { Text("완료", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
    ProfileAvatar()
  }
}

@Composable
private fun ProfileAvatar() {
  Surface(
    modifier = Modifier.size(28.dp),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Box(contentAlignment = Alignment.Center) { Text("🐦", fontSize = 13.sp) }
  }
}

@Composable
private fun AnonymousSetting(checked: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.primaryContainer)
        .clickable(onClick = onClick)
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
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
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
  editing: Boolean,
  onEdit: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 0.dp, end = 10.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(countLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    Spacer(Modifier.weight(1f))
    Text("최근 기록순⌄", style = MaterialTheme.typography.labelMedium)
    if (!editing) {
      TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp)) {
        Text("편집", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun LibraryBookRow(
  book: ArchivedBook,
  editing: Boolean,
  selected: Boolean,
  onSelect: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
        .then(if (editing) Modifier.clickable(onClick = onSelect) else Modifier)
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
      Box(contentAlignment = Alignment.Center) {
        Text("책", style = MaterialTheme.typography.titleMedium)
        AsyncImage(
          model = book.coverUrl,
          contentDescription = book.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
      ReadingStatusTag(book.status)
      Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        listOf(book.creator, book.category.ifBlank { book.publisher }).filter { it.isNotBlank() }.joinToString(" · "),
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
          modifier =
            Modifier
              .fillMaxWidth(book.progressRatio.coerceIn(0f, 1f))
              .height(2.dp)
              .background(MaterialTheme.colorScheme.onSurface),
        )
      }
    }
    if (editing) {
      Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onDelete),
        contentAlignment = Alignment.Center,
      ) {
        Text("⌫", color = MaterialTheme.colorScheme.error, fontSize = 20.sp)
      }
    } else {
      Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 24.sp)
    }
  }
}

@Composable
private fun SelectionBox(selected: Boolean) {
  Box(
    modifier =
      Modifier
        .size(20.dp)
        .background(if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
        .border(1.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center,
  ) {
    if (selected) Text("✓", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp)
  }
}

@Composable
private fun ReadingStatusTag(status: ReadingStatus) {
  val selected = status == ReadingStatus.Reading
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
      Text("발견에서 읽고 싶은 책을 등록해 보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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
      Text("TOP", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
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
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onBackground,
            contentColor = MaterialTheme.colorScheme.surface,
          ),
      ) { Text("서재에서 삭제") }
    }
  }
}

@Composable
private fun StatusChangeDialog(
  selectedCount: Int,
  onDismiss: () -> Unit,
  onChange: (ReadingStatus) -> Unit,
) {
  var selected by rememberSaveable { mutableStateOf(ReadingStatus.Reading) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("독서 상태 변경") },
    text = {
      Column {
        Text("선택한 ${selectedCount}권의 상태를 변경합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        ReadingStatus.entries.forEach { status ->
          Row(
            modifier = Modifier.fillMaxWidth().clickable { selected = status }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = selected == status, onClick = { selected = status })
            Text(status.label)
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = { onChange(selected) }) { Text("변경") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
  )
}

@Composable
private fun NicknameDialog(
  nickname: String,
  onNicknameChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("닉네임 설정") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("기록과 감상에 표시할 닉네임이 필요해요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
          value = nickname,
          onValueChange = onNicknameChange,
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("닉네임을 입력하세요") },
          supportingText = { Text("${nickname.length}/10") },
          singleLine = true,
        )
      }
    },
    confirmButton = { TextButton(onClick = onConfirm, enabled = nickname.isNotBlank()) { Text("확인") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
  )
}
