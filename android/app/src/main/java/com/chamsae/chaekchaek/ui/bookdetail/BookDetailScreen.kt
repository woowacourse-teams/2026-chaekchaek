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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.chamsae.chaekchaek.auth.AuthSession
import com.chamsae.chaekchaek.auth.requestGoogleIdToken
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ReadingStatus
import com.chamsae.chaekchaek.theme.ChaekAccent
import com.chamsae.chaekchaek.theme.ChaekAccentInk
import com.chamsae.chaekchaek.theme.ChaekAccentSoft
import com.chamsae.chaekchaek.theme.ChaekBand
import com.chamsae.chaekchaek.theme.ChaekInk
import com.chamsae.chaekchaek.theme.ChaekInkSecondary
import com.chamsae.chaekchaek.theme.ChaekSurface
import com.chamsae.chaekchaek.ui.home.coverResource
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
import kotlinx.coroutines.launch

@Composable
fun BookDetailRoute(
  book: BookDetailArgs,
  bookDetailRepository: BookDetailRemoteRepository,
  mobileAuthRepository: MobileAuthRemoteRepository,
  authSession: AuthSession,
  libraryRepository: LibraryRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens by authSession.tokens.collectAsStateWithLifecycle()
  val archivedBooks by libraryRepository.items.collectAsStateWithLifecycle()
  var detail by remember(book.isbn13) { mutableStateOf<BookDetail?>(null) }
  var reviews by remember(book.isbn13) { mutableStateOf(emptyList<BookReview>()) }
  var reviewCount by remember(book.isbn13) { mutableStateOf(0) }
  var reviewsLoading by remember(book.isbn13) { mutableStateOf(false) }
  var reviewScope by rememberSaveable(book.isbn13) { mutableStateOf(ReviewScope.ALL) }
  var reviewSort by rememberSaveable(book.isbn13) { mutableStateOf(ReviewSort.LATEST) }
  val archivedBook = archivedBooks.firstOrNull { it.id == book.id }

  LaunchedEffect(book.isbn13) {
    detail = book.isbn13.takeIf(String::isNotBlank)?.let { runCatching { bookDetailRepository.detail(it) }.getOrNull() }
  }
  LaunchedEffect(detail?.bookId, reviewScope, reviewSort, tokens?.accessToken) {
    val bookId = detail?.bookId ?: return@LaunchedEffect
    reviewsLoading = true
    runCatching { bookDetailRepository.reviews(bookId, reviewScope, reviewSort, tokens?.accessToken) }
      .onSuccess {
        reviewCount = it.totalCount
        reviews = it.items
      }.onFailure {
        reviewCount = 0
        reviews = emptyList()
      }
    reviewsLoading = false
  }
  val displayBook = detail?.toBookDetailArgs(book) ?: archivedBook?.toBookDetailArgs() ?: book

  BookDetailScreen(
    book = displayBook,
    archivedBook = archivedBook,
    averageRating = detail?.averageRating,
    ratingCount = detail?.ratingCount,
    reviews = reviews,
    reviewCount = reviewCount,
    reviewsLoading = reviewsLoading,
    reviewScope = reviewScope,
    reviewSort = reviewSort,
    onBack = onBack,
    onReviewScopeChange = { reviewScope = it },
    onReviewSortChange = { reviewSort = it },
    signedIn = tokens != null,
    onGoogleSignIn = { idToken -> authSession.signIn(mobileAuthRepository.loginWithGoogle(idToken)) },
    modifier = modifier,
  )
}

@Composable
fun BookDetailScreen(
  book: BookDetailArgs,
  archivedBook: ArchivedBook?,
  averageRating: Double? = null,
  ratingCount: Int? = null,
  reviews: List<BookReview> = emptyList(),
  reviewCount: Int = 0,
  reviewsLoading: Boolean = false,
  reviewScope: ReviewScope = ReviewScope.ALL,
  reviewSort: ReviewSort = ReviewSort.LATEST,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onReviewScopeChange: (ReviewScope) -> Unit = {},
  onReviewSortChange: (ReviewSort) -> Unit = {},
  signedIn: Boolean = false,
  onGoogleSignIn: suspend (String) -> Unit = {},
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
  var showLoginSheet by rememberSaveable { mutableStateOf(false) }
  var openMineAfterLogin by remember { mutableStateOf(false) }
  val context = LocalContext.current
  var signingIn by rememberSaveable { mutableStateOf(false) }
  var loginError by rememberSaveable { mutableStateOf<String?>(null) }
  val requireAuthentication = {
    if (!signedIn) showLoginSheet = true
  }

  Box(
    modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 82.dp),
    ) {
      item { ArchiveStage(book, onBack, requireAuthentication) }
      item { BookSummary(book, averageRating, ratingCount) }
      item { ReadingRecord(book, archivedBook, requireAuthentication) }
      item { Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand)) }
      item {
        ReviewsSection(
          reviews = reviews,
          reviewCount = reviewCount,
          loading = reviewsLoading,
          scope = reviewScope,
          sort = reviewSort,
          onScopeChange = { requested ->
            if (requested == ReviewScope.MINE && !signedIn) {
              openMineAfterLogin = true
              showLoginSheet = true
            } else {
              onReviewScopeChange(requested)
            }
          },
          onSortChange = onReviewSortChange,
          onAuthenticationRequired = requireAuthentication,
        )
      }
    }

    ComposeBar(
      onClick = requireAuthentication,
      modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
    )

    if (showScrollTop) {
      ScrollTopButton(
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 72.dp),
      )
    }
  }

  if (showLoginSheet) {
    LoginRequiredSheet(
      signingIn = signingIn,
      error = loginError,
      onDismiss = { if (!signingIn) showLoginSheet = false },
      onGoogleSignIn = {
        if (signingIn) return@LoginRequiredSheet
        scope.launch {
          signingIn = true
          loginError = null
          runCatching { requestGoogleIdToken(context) }
            .mapCatching { idToken -> onGoogleSignIn(idToken) }
            .onSuccess {
              if (openMineAfterLogin) onReviewScopeChange(ReviewScope.MINE)
              openMineAfterLogin = false
              showLoginSheet = false
            }
            .onFailure { loginError = "로그인하지 못했어요. 다시 시도해 주세요." }
          signingIn = false
        }
      },
    )
  }
}

@Composable
private fun ArchiveStage(book: BookDetailArgs, onBack: () -> Unit, onLibraryClick: () -> Unit) {
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
      onClick = onLibraryClick,
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
private fun BookSummary(book: BookDetailArgs, averageRating: Double?, ratingCount: Int?) {
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
      Text(averageRating?.let { "%.1f".format(it) } ?: "평점 없음", style = MaterialTheme.typography.labelMedium)
      ratingCount?.let { Text("${it}명", style = MaterialTheme.typography.labelSmall) }
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
private fun ReadingRecord(book: BookDetailArgs, archivedBook: ArchivedBook?, onAuthenticationRequired: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("내 독서 기록", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.weight(1f))
      Surface(onClick = onAuthenticationRequired, color = Color.Transparent) {
        Text(
          "☆ 별점 주기",
          modifier = Modifier.padding(8.dp),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.labelMedium,
        )
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      ReadingStatus.entries.forEach { status ->
        val selected = archivedBook?.status == status
        Surface(
          onClick = onAuthenticationRequired,
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
        onClick = onAuthenticationRequired,
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
        onClick = onAuthenticationRequired,
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
private fun ReviewsSection(
  reviews: List<BookReview>,
  reviewCount: Int,
  loading: Boolean,
  scope: ReviewScope,
  sort: ReviewSort,
  onScopeChange: (ReviewScope) -> Unit,
  onSortChange: (ReviewSort) -> Unit,
  onAuthenticationRequired: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("감상 $reviewCount", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.weight(1f))
      Surface(
        onClick = { onSortChange(if (sort == ReviewSort.LATEST) ReviewSort.PAGE else ReviewSort.LATEST) },
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) {
          Text(if (sort == ReviewSort.LATEST) "최신순⌄" else "페이지순⌄", style = MaterialTheme.typography.labelSmall)
        }
      }
      Spacer(Modifier.width(6.dp))
      Surface(shape = RoundedCornerShape(4.dp), color = ChaekInk) {
        Row {
          Text(
            "전체 피드",
            modifier = Modifier.clickable { onScopeChange(ReviewScope.ALL) }.padding(horizontal = 9.dp, vertical = 7.dp),
            color = if (scope == ReviewScope.ALL) ChaekSurface else ChaekInkSecondary,
            style = MaterialTheme.typography.labelSmall,
          )
          Text(
            "내 피드",
            modifier = Modifier.clickable { onScopeChange(ReviewScope.MINE) }.padding(horizontal = 9.dp, vertical = 7.dp),
            color = if (scope == ReviewScope.MINE) ChaekSurface else ChaekInkSecondary,
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }
    Spacer(Modifier.height(12.dp))
    when {
      loading -> Text("감상을 불러오는 중이에요", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
      reviews.isEmpty() -> Text("아직 등록된 감상이 없어요", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.bodyMedium)
      else -> reviews.forEach { ReviewCard(it, onAuthenticationRequired) }
    }
  }
}

@Composable
private fun ReviewCard(review: BookReview, onAuthenticationRequired: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(modifier = Modifier.size(30.dp), shape = CircleShape, color = ChaekAccentSoft) {
        Box(contentAlignment = Alignment.Center) { Text(review.authorName.take(1)) }
      }
      Column(modifier = Modifier.padding(start = 8.dp)) {
        Text(if (review.anonymous) "${review.authorName} (익명)" else review.authorName, style = MaterialTheme.typography.titleSmall)
        Text(
          "${review.createdAt.take(10).replace('-', '.')} · ${review.currentPage?.let { "p.${it}까지" }.orEmpty()}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
    Text(review.content, style = MaterialTheme.typography.bodyMedium)
    review.quote?.let { quote ->
      Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)) {
        Text("인용 위치 · ${review.currentPage?.let { "p.$it" }.orEmpty()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text("“$quote”", style = MaterialTheme.typography.bodySmall)
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
      Text("♡ 좋아요 ${review.likeCount}", modifier = Modifier.clickable(onClick = onAuthenticationRequired), style = MaterialTheme.typography.labelSmall)
      Row(modifier = Modifier.clickable(role = Role.Button, onClick = onAuthenticationRequired), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_comment), contentDescription = "감상에 답글 작성", modifier = Modifier.size(14.dp))
        Text("답글 ${review.replyCount}", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LoginRequiredSheet(
  signingIn: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onGoogleSignIn: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("로그인이 필요해요", style = MaterialTheme.typography.titleLarge)
      Text("내 독서 기록을 남기고 감상에 참여하려면 로그인해 주세요.", style = MaterialTheme.typography.bodyMedium)
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
      Surface(
        onClick = onGoogleSignIn,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = ChaekInk,
      ) {
        Text(
          if (signingIn) "로그인 중..." else "Google로 계속하기",
          modifier = Modifier.padding(vertical = 14.dp),
          color = ChaekSurface,
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

private fun BookDetail.toBookDetailArgs(fallback: BookDetailArgs) =
  fallback.copy(
    isbn13 = isbn13,
    bookId = bookId,
    title = title,
    creator = (authors + translators.map { "$it 옮김" }).joinToString(" · "),
    publisher = publisher,
    year = publishedDate?.take(4).orEmpty(),
    category = category,
    totalPages = totalPages ?: 0,
    coverUrl = coverImageUrl,
  )

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
