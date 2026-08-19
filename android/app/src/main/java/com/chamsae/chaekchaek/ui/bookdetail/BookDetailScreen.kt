package com.chamsae.chaekchaek.ui.bookdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.chamsae.chaekchaek.R
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.BookReflection
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ReflectionRepository
import com.chamsae.chaekchaek.data.ReadingStatus
import com.chamsae.chaekchaek.theme.ChaekAccent
import com.chamsae.chaekchaek.theme.ChaekAccentInk
import com.chamsae.chaekchaek.theme.ChaekAccentSoft
import com.chamsae.chaekchaek.theme.ChaekBand
import com.chamsae.chaekchaek.theme.ChaekInk
import com.chamsae.chaekchaek.theme.ChaekInkSecondary
import com.chamsae.chaekchaek.theme.ChaekSurface
import com.chamsae.chaekchaek.ui.home.coverResource
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Composable
fun BookDetailRoute(
  book: BookDetailArgs,
  libraryRepository: LibraryRepository,
  reflectionRepository: ReflectionRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val archivedBooks by libraryRepository.items.collectAsStateWithLifecycle()
  val reflections by reflectionRepository.reflections.collectAsStateWithLifecycle()
  val anonymous by libraryRepository.anonymousReviews.collectAsStateWithLifecycle()
  val nickname by libraryRepository.nickname.collectAsStateWithLifecycle()
  val archivedBook = archivedBooks.firstOrNull { it.id == book.id }
  val displayBook = archivedBook?.toBookDetailArgs() ?: book

  BookDetailScreen(
    book = displayBook,
    archivedBook = archivedBook,
    reflections = reflections.filter { it.bookId == book.id },
    anonymous = anonymous,
    authorName = nickname,
    onBack = onBack,
    onSubmitReflection = { draft ->
      reflectionRepository.add(
        BookReflection(
          id = UUID.randomUUID().toString(),
          bookId = book.id,
          body = draft.body.trim(),
          quote = draft.quote.trim(),
          page = draft.page.toIntOrNull(),
          chapter = draft.chapter.trim(),
          spoiler = draft.spoiler,
          anonymous = anonymous,
          authorName = if (anonymous) "" else nickname,
          createdAt = System.currentTimeMillis(),
        ),
      )
    },
    modifier = modifier,
  )
}

@Composable
fun BookDetailScreen(
  book: BookDetailArgs,
  archivedBook: ArchivedBook?,
  reflections: List<BookReflection> = emptyList(),
  anonymous: Boolean = true,
  authorName: String = "",
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onSubmitReflection: (ReflectionDraft) -> Unit = {},
  onRate: () -> Unit = {},
) {
  BackHandler(onBack = onBack)
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val threshold = remember(density) { with(density) { 240.dp.roundToPx() } }
  val showScrollTop by remember(threshold) {
    derivedStateOf {
      listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= threshold
    }
  }
  var showReflectionSheet by rememberSaveable { mutableStateOf(false) }

  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 82.dp),
    ) {
      item { ArchiveStage(book, onBack) }
      item { BookSummary(book) }
      item { ReadingRecord(book, archivedBook, onRate) }
      item { Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand)) }
      item { ReviewsSection(reflections) }
    }

    ComposeBar(
      onClick = { showReflectionSheet = true },
      modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
    )

    if (showScrollTop) {
      ScrollTopButton(
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 72.dp),
      )
    }
  }

  if (showReflectionSheet) {
    ReflectionSheet(
      initialPage = archivedBook?.currentPage?.takeIf { it > 0 }?.toString().orEmpty(),
      anonymous = anonymous,
      authorName = authorName,
      onDismiss = { showReflectionSheet = false },
      onSubmit = {
        onSubmitReflection(it)
        showReflectionSheet = false
      },
    )
  }
}

@Composable
private fun ArchiveStage(book: BookDetailArgs, onBack: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(298.dp).background(ChaekInk),
  ) {
    Surface(
      onClick = onBack,
      modifier = Modifier.offset(16.dp, 8.dp).size(38.dp),
      shape = RoundedCornerShape(4.dp),
      color = Color.Transparent,
      border = BorderStroke(1.dp, ChaekSurface),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter = painterResource(R.drawable.ic_back),
          contentDescription = "뒤로 가기",
          modifier = Modifier.size(20.dp),
          tint = ChaekSurface,
        )
      }
    }

    Surface(
      modifier = Modifier.align(Alignment.TopEnd).offset((-16).dp, 8.dp).size(38.dp),
      shape = RoundedCornerShape(4.dp),
      color = ChaekAccent,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text("⌑", color = ChaekInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
      }
    }

    Text(
      "ARCHIVE\nSTORIES\n2026",
      modifier = Modifier.offset(16.dp, 78.dp),
      color = ChaekAccent,
      fontFamily = FontFamily.Monospace,
      fontSize = 9.sp,
      lineHeight = 14.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
    )

    Box(
      modifier =
        Modifier
          .align(Alignment.Center)
          .offset(y = 5.dp)
          .size(width = 164.dp, height = 217.dp)
          .background(ChaekSurface)
          .border(1.dp, ChaekSurface)
          .shadow(10.dp, spotColor = ChaekAccent.copy(alpha = 0.4f)),
      contentAlignment = Alignment.Center,
    ) {
      BookCover(
        book = book,
        modifier = Modifier.size(width = 130.dp, height = 194.dp),
      )
    }

    Surface(
      modifier = Modifier.align(Alignment.BottomEnd).offset((-28).dp, (-22).dp),
      shape = RoundedCornerShape(4.dp),
      color = Color.Transparent,
      border = BorderStroke(3.dp, ChaekAccent),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text("MOST READ", color = ChaekAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text("30 NOTES / TODAY", color = ChaekAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun BookCover(book: BookDetailArgs, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, ChaekInk),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text("책", style = MaterialTheme.typography.titleMedium)
      if (book.coverUrl.isNotBlank()) {
        AsyncImage(
          model = book.coverUrl,
          contentDescription = "${book.title} 표지",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      } else {
        Image(
          painter = painterResource(coverResource(book.coverId)),
          contentDescription = "${book.title} 표지",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
    }
  }
}

@Composable
private fun BookSummary(book: BookDetailArgs) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      book.title,
      style = MaterialTheme.typography.headlineLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
    Text(
      listOf(book.creator, book.publisher).filter(String::isNotBlank).joinToString(" · ").ifBlank { "책 정보 준비 중" },
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodySmall,
      textAlign = TextAlign.Center,
    )
    Row(
      modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.Center,
    ) {
      listOf(
        book.category,
        book.year.takeIf(String::isNotBlank)?.let { "${it} 초판" }.orEmpty(),
        book.totalPages.takeIf { it > 0 }?.let { "${it}쪽" }.orEmpty(),
      ).filter(String::isNotBlank).forEach { MetaChip(it) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("★★★★☆", color = ChaekAccent, fontSize = 14.sp, letterSpacing = 1.sp)
      Text("4.2", style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
private fun MetaChip(label: String) {
  Surface(
    modifier = Modifier.padding(horizontal = 3.dp).height(24.dp),
    shape = RoundedCornerShape(4.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
      Text(label, style = MaterialTheme.typography.labelSmall)
    }
  }
}

@Composable
private fun ReadingRecord(book: BookDetailArgs, archivedBook: ArchivedBook?, onRate: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("내 독서 기록", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.weight(1f))
      Surface(onClick = onRate, color = Color.Transparent) {
        Text("☆ 별점 주기", modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.labelMedium)
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      ReadingStatus.entries.forEach { status ->
        val selected = archivedBook?.status == status
        Surface(
          modifier = Modifier.weight(1f).height(32.dp),
          shape = RoundedCornerShape(4.dp),
          color = if (selected) ChaekInk else MaterialTheme.colorScheme.surface,
          contentColor = if (selected) ChaekSurface else MaterialTheme.colorScheme.onSurface,
          border = BorderStroke(1.dp, if (selected) ChaekInk else MaterialTheme.colorScheme.outline),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(status.label, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Surface(
        modifier = Modifier.width(90.dp).height(38.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("⌑", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${archivedBook?.currentPage ?: 0}", style = MaterialTheme.typography.bodyMedium)
        }
      }
      Text(
        if (book.totalPages > 0) " / ${book.totalPages}쪽" else " / 쪽수 미정",
        modifier = Modifier.padding(start = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
      )
      Spacer(Modifier.weight(1f))
      Surface(
        modifier = Modifier.height(38.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
      ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
          Text("✎  쪽수 입력", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
    Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
      Box(
        Modifier
          .fillMaxWidth(archivedBook?.progressRatio?.coerceIn(0f, 1f) ?: 0f)
          .height(3.dp)
          .background(ChaekAccent),
      )
    }
  }
}

@Composable
private fun ReviewsSection(reflections: List<BookReflection>) {
  // ponytail: Pencil 샘플 데이터 - 상세/감상 API가 연결되면 저장소 결과로 교체한다.
  Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("감상 ${30 + reflections.size}", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.weight(1f))
      Surface(
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
          Text("최신순⌄", style = MaterialTheme.typography.labelSmall)
        }
      }
      Spacer(Modifier.width(6.dp))
      Surface(shape = RoundedCornerShape(4.dp), color = ChaekInk) {
        Row {
          Text("전체 피드", modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = ChaekSurface, style = MaterialTheme.typography.labelSmall)
          Text("내 피드", modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = ChaekInkSecondary, style = MaterialTheme.typography.labelSmall)
        }
      }
    }
    Spacer(Modifier.height(12.dp))
    reflections.forEach { reflection ->
      ReviewCard(
        name = if (reflection.anonymous) "익명의 참새" else reflection.authorName.ifBlank { "참새" },
        date = formatReflectionDate(reflection.createdAt),
        position =
          listOfNotNull(
            reflection.page?.let { "p.${it}까지" },
            reflection.chapter.takeIf(String::isNotBlank),
          ).joinToString(" · ").ifBlank { "독서 위치 미입력" },
        body = reflection.body,
        quote = reflection.quote,
        avatar = R.drawable.avatar_kim,
        replies = emptyList(),
        spoiler = reflection.spoiler,
      )
      Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand))
    }
    ReviewCard(
      name = "참새 1204 (익명)",
      date = "2026.08.05",
      position = "p.80까지",
      body = "혼자 남겨진 사람이 절망 대신 문제를 하나씩 풀어가는 태도가 인상 깊었다.",
      quote = "나는 이 행성에서 과학으로 살아남을 것이다.",
      avatar = R.drawable.avatar_kim,
      replies = listOf(
        "다정한 참새" to "끝까지 유머를 잃지 않는 점도 좋았어요.",
        "느긋한 참새" to "저도 같은 문장에서 오래 멈췄습니다.",
      ),
    )
    Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand))
    ReviewCard(
      name = "짹짹짹",
      date = "2026.08.03",
      position = "p.160까지",
      body = "실패를 기록하고 다음 실험으로 넘어가는 과정이 이 책의 가장 큰 매력이었다.",
      quote = "문제를 해결하려면 먼저 정확히 측정해야 한다.",
      avatar = R.drawable.avatar_yoon,
      replies = listOf("성실한 참새" to "읽는 내내 응원하게 되는 인물이었어요."),
    )
  }
}

internal fun formatReflectionDate(createdAt: Long): String =
  DateTimeFormatter
    .ofPattern("yyyy.MM.dd", Locale.KOREA)
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(createdAt))

@Composable
private fun ReviewCard(
  name: String,
  date: String,
  position: String,
  body: String,
  quote: String,
  avatar: Int,
  replies: List<Pair<String, String>>,
  spoiler: Boolean = false,
) {
  Column(
    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(avatar),
        contentDescription = null,
        modifier = Modifier.size(30.dp).clip(CircleShape),
      )
      Column(modifier = Modifier.padding(start = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(name, style = MaterialTheme.typography.titleSmall)
          Surface(shape = RoundedCornerShape(4.dp), color = ChaekAccentSoft) {
            Text("✓ 완독", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = ChaekAccentInk, style = MaterialTheme.typography.labelSmall)
          }
          if (spoiler) {
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.errorContainer) {
              Text("스포일러", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall)
            }
          }
        }
        Text("$date · $position", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.weight(1f))
      Text("⋯", fontSize = 20.sp)
    }
    Text(body, style = MaterialTheme.typography.bodyMedium)
    if (quote.isNotBlank()) {
      Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text("인용 위치 · $position", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text("“$quote”", style = MaterialTheme.typography.bodySmall)
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
      Text("♡ 좋아요 12", style = MaterialTheme.typography.labelSmall)
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(painterResource(R.drawable.ic_comment), contentDescription = null, modifier = Modifier.size(14.dp))
        Text("답글 ${replies.size}", style = MaterialTheme.typography.labelSmall)
      }
    }
    replies.forEach { (replyName, reply) ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Surface(modifier = Modifier.size(22.dp), shape = CircleShape, color = ChaekAccentSoft) {}
        Column(
          modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(9.dp),
        ) {
          Text(replyName, style = MaterialTheme.typography.labelSmall)
          Text(reply, style = MaterialTheme.typography.bodySmall)
        }
        Text("♡ 2", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

data class ReflectionDraft(
  val body: String,
  val quote: String,
  val page: String,
  val chapter: String,
  val spoiler: Boolean,
)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun ReflectionSheet(
  initialPage: String,
  anonymous: Boolean,
  authorName: String,
  onDismiss: () -> Unit,
  onSubmit: (ReflectionDraft) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var body by rememberSaveable { mutableStateOf("") }
  var quote by rememberSaveable { mutableStateOf("") }
  var page by rememberSaveable { mutableStateOf(initialPage) }
  var chapter by rememberSaveable { mutableStateOf("") }
  var spoiler by rememberSaveable { mutableStateOf(false) }
  val canSubmit = body.isNotBlank()
  val scrollState = rememberScrollState()
  val imeVisible = WindowInsets.isImeVisible

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    modifier = Modifier.wrapContentHeight(),
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    dragHandle = {
      Box(
        Modifier
          .padding(top = 8.dp, bottom = 4.dp)
          .size(width = 24.dp, height = 3.dp)
          .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
      )
    },
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .then(if (imeVisible) Modifier.verticalScroll(scrollState).imePadding() else Modifier)
          .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("감상 남기기", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        Surface(onClick = onDismiss, color = Color.Transparent, shape = CircleShape) {
          Text("×", modifier = Modifier.padding(8.dp), fontSize = 20.sp)
        }
      }
      ReflectionFieldLabel("느낀점", required = true)
      ReflectionTextField(
        value = body,
        onValueChange = { body = it },
        placeholder = "이 구간을 읽으며 든 생각을 남겨보세요",
        modifier = Modifier.fillMaxWidth().height(108.dp),
      )
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = spoiler, onCheckedChange = { spoiler = it })
          Text("스포일러", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
        }
      }
      ReflectionFieldLabel("인상 깊은 문구")
      ReflectionTextField(
        value = quote,
        onValueChange = { quote = it },
        placeholder = "기억하고 싶은 문장을 옮겨 적어보세요",
        modifier = Modifier.fillMaxWidth().height(76.dp),
      )
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          ReflectionFieldLabel("쪽수")
          ReflectionTextField(
            value = page,
            onValueChange = { input -> page = input.filter(Char::isDigit) },
            placeholder = "80 쪽",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
          )
        }
        Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          ReflectionFieldLabel("목차 / 챕터")
          ReflectionTextField(
            value = chapter,
            onValueChange = { chapter = it },
            placeholder = "Chapter 1",
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )
        }
      }
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp),
      ) {
        Text(
          if (anonymous) "⌁  익명 · 프로필은 공개되지 않아요" else "⌁  ${authorName.ifBlank { "닉네임" }}으로 공개됩니다",
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Surface(
        onClick = {
          onSubmit(ReflectionDraft(body, quote, page, chapter, spoiler))
        },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (canSubmit) ChaekInk else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (canSubmit) ChaekSurface else MaterialTheme.colorScheme.onSurfaceVariant,
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text("감상 남기기", style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

@Composable
private fun ReflectionFieldLabel(label: String, required: Boolean = false) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    if (required) {
      Surface(shape = CircleShape, color = ChaekInk) {
        Text("필수", modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), color = ChaekSurface, fontSize = 8.sp)
      }
    }
  }
}

@Composable
private fun ReflectionTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  singleLine: Boolean = false,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
    textStyle = MaterialTheme.typography.bodyMedium,
    keyboardOptions = keyboardOptions,
    singleLine = singleLine,
    shape = RoundedCornerShape(4.dp),
    colors =
      TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedIndicatorColor = ChaekInk,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
      ),
  )
}

@Composable
private fun ComposeBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().height(48.dp).shadow(8.dp, RoundedCornerShape(24.dp)).clickable(role = Role.Button, onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("✎", fontSize = 16.sp)
      Text(
        "이 순간의 감상 남기기",
        modifier = Modifier.padding(start = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
      Spacer(Modifier.weight(1f))
      Text("➤", color = ChaekAccent)
    }
  }
}

@Composable
private fun ScrollTopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Surface(
    onClick = onClick,
    modifier = modifier.size(48.dp).shadow(6.dp, CircleShape),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
      Text("⌃", fontSize = 15.sp, lineHeight = 12.sp)
      Text("TOP", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
    }
  }
}
