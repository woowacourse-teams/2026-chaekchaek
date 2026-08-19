package com.chamsae.chaekchaek.ui.bookdetail

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.chamsae.chaekchaek.theme.ChaekBackground
import com.chamsae.chaekchaek.theme.ChaekBorder
import com.chamsae.chaekchaek.theme.ChaekInk
import com.chamsae.chaekchaek.theme.ChaekInkSecondary
import com.chamsae.chaekchaek.theme.ChaekSurface
import com.chamsae.chaekchaek.theme.ChaekSurfaceMuted
import com.chamsae.chaekchaek.ui.common.withDelayedApiLoading
import com.chamsae.chaekchaek.ui.home.coverResource
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.MobileLoginException
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
import com.chaekchaek.app.domain.rating.Rating
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
  var reloadNonce by remember { mutableStateOf(0) }
  val archivedBook = archivedBooks.firstOrNull { it.id == book.id }

  suspend fun bookIdForWrite(accessToken: String): Long =
    detail?.bookId ?: requireNotNull(
      bookDetailRepository.addToLibrary(book.isbn13, detail?.totalPages, accessToken).bookId,
    )

  LaunchedEffect(book.isbn13, tokens?.accessToken, reloadNonce) {
    detail = book.isbn13.takeIf(String::isNotBlank)?.let { runCatching { bookDetailRepository.detail(it, tokens?.accessToken) }.getOrNull() }
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
    reviews = reviews,
    reviewCount = reviewCount,
    reviewsLoading = reviewsLoading,
    reviewScope = reviewScope,
    reviewSort = reviewSort,
    myRecord = detail?.myRecord,
    onBack = onBack,
    onReviewScopeChange = { reviewScope = it },
    onReviewSortChange = { reviewSort = it },
    signedIn = tokens != null,
    onGoogleSignIn = { idToken -> authSession.signIn(mobileAuthRepository.loginWithGoogle(idToken)) },
    onAddToLibrary = {
      bookDetailRepository.addToLibrary(book.isbn13, detail?.totalPages, requireNotNull(tokens).accessToken)
      reloadNonce++
    },
    onStatusChange = { status ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.updateReadingStatus(bookIdForWrite(accessToken), status.toApiStatus(), accessToken)
      libraryRepository.changeStatus(setOf(book.id), status)
      reloadNonce++
    },
    onPageSave = { page ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.updateCurrentPage(bookIdForWrite(accessToken), page, detail?.totalPages, accessToken)
      reloadNonce++
    },
    onRatingSave = { rating ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.rate(bookIdForWrite(accessToken), rating.score.toDouble(), accessToken)
      reloadNonce++
    },
    onReviewCreate = { content ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.createReview(bookIdForWrite(accessToken), content, accessToken)
      reloadNonce++
    },
    onReviewLike = { reviewId ->
      bookDetailRepository.likeReview(reviewId, requireNotNull(tokens).accessToken)
      reloadNonce++
    },
    onReplyCreate = { reviewId, content ->
      bookDetailRepository.createReply(reviewId, content, requireNotNull(tokens).accessToken)
      reloadNonce++
    },
    modifier = modifier,
  )
}

@Composable
fun BookDetailScreen(
  book: BookDetailArgs,
  archivedBook: ArchivedBook?,
  averageRating: Double? = null,
  reviews: List<BookReview> = emptyList(),
  reviewCount: Int = 0,
  reviewsLoading: Boolean = false,
  reviewScope: ReviewScope = ReviewScope.ALL,
  reviewSort: ReviewSort = ReviewSort.LATEST,
  myRecord: LibraryRecord? = null,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onReviewScopeChange: (ReviewScope) -> Unit = {},
  onReviewSortChange: (ReviewSort) -> Unit = {},
  signedIn: Boolean = false,
  onGoogleSignIn: suspend (String) -> Unit = {},
  onAddToLibrary: suspend () -> Unit = {},
  onStatusChange: suspend (ReadingStatus) -> Unit = {},
  onPageSave: suspend (Int) -> Unit = {},
  onRatingSave: suspend (Rating) -> Unit = {},
  onReviewCreate: suspend (String) -> Unit = {},
  onReviewLike: suspend (Long) -> Unit = {},
  onReplyCreate: suspend (Long, String) -> Unit = { _, _ -> },
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
  var requestInFlight by remember { mutableStateOf(false) }
  var showRequestLoading by remember { mutableStateOf(false) }
  var showRatingDialog by rememberSaveable { mutableStateOf(false) }
  var showPageSheet by rememberSaveable { mutableStateOf(false) }
  var showReviewSheet by rememberSaveable { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingAction by remember { mutableStateOf<Pair<String, suspend () -> Unit>?>(null) }
  fun execute(operation: String, action: suspend () -> Unit) {
    if (requestInFlight) return
    requestInFlight = true
    scope.launch {
      try {
        runCatching { withDelayedApiLoading({ showRequestLoading = it }, action) }
          .onFailure { error ->
            val httpStatus = Regex("\\b[1-5]\\d{2}\\b").find(error.message.orEmpty())?.value
            Log.w("ChaekchaekApi", "Book detail $operation failed: ${error::class.simpleName}, HTTP ${httpStatus ?: "N/A"}")
            snackbarHostState.showSnackbar("요청을 처리하지 못했어요. 다시 시도해 주세요.")
          }
      } finally {
        showRequestLoading = false
        requestInFlight = false
      }
    }
  }
  fun runAuthenticated(operation: String, action: suspend () -> Unit) {
    if (!signedIn) {
      pendingAction = operation to action
      showLoginSheet = true
      return
    }
    execute(operation, action)
  }

  Box(
    modifier = modifier.fillMaxSize().background(ChaekBackground).navigationBarsPadding(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 82.dp),
    ) {
      item { ArchiveStage(book, onBack) { runAuthenticated("보관") { onAddToLibrary() } } }
      item { BookSummary(book, averageRating) }
      item {
        ReadingRecord(
          book = book,
          record = myRecord,
          onRate = { if (signedIn) showRatingDialog = true else showLoginSheet = true },
          onStatusChange = { status -> runAuthenticated("독서 상태 변경") { onStatusChange(status) } },
          onPageInput = { if (signedIn) showPageSheet = true else showLoginSheet = true },
        )
      }
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
          onLike = { reviewId -> runAuthenticated("감상 반응") { onReviewLike(reviewId) } },
          onReply = { reviewId, content -> runAuthenticated("답글 작성") { onReplyCreate(reviewId, content) } },
        )
      }
    }

    ComposeBar(
      onClick = { if (signedIn) showReviewSheet = true else showLoginSheet = true },
      modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
    )

    if (showScrollTop) {
      ScrollTopButton(
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 72.dp),
      )
    }

    if (showRequestLoading) {
      RequestLoadingOverlay(Modifier.fillMaxSize())
    }
  }

  SnackbarHost(hostState = snackbarHostState)

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
              pendingAction?.let { execute(it.first, it.second) }
              pendingAction = null
              showLoginSheet = false
            }
            .onFailure {
              val code = (it as? MobileLoginException)?.code ?: it::class.simpleName.orEmpty()
              Log.w("ChaekchaekAuth", "Google login failed: $code")
              loginError = "로그인하지 못했어요. 다시 시도해 주세요."
            }
          signingIn = false
        }
      },
    )
  }

  if (showRatingDialog) {
    BookRatingDialog(
      currentBookId = book.bookId?.toString().orEmpty(),
      initialRating = myRecord?.rating?.let { Rating.ofScore(it.toFloat()) },
      recentRatings = emptyList(),
      onDismiss = { showRatingDialog = false },
      onSave = { rating ->
        runAuthenticated("별점 저장") { onRatingSave(rating) }
        showRatingDialog = false
      },
    )
  }
  if (showPageSheet) {
    PageInputSheet(
      initialPage = myRecord?.currentPage ?: 0,
      totalPages = book.totalPages,
      onDismiss = { showPageSheet = false },
      onSave = { page ->
        runAuthenticated("쪽수 저장") { onPageSave(page) }
        showPageSheet = false
      },
    )
  }
  if (showReviewSheet) {
    ReviewInputSheet(
      onDismiss = { showReviewSheet = false },
      onSave = { content ->
        runAuthenticated("감상 작성") { onReviewCreate(content) }
        showReviewSheet = false
      },
    )
  }
}

@Composable
private fun RequestLoadingOverlay(modifier: Modifier = Modifier) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    modifier =
      modifier
        .background(ChaekInk.copy(alpha = 0.15f))
        .clickable(interactionSource = interactionSource, indication = null, onClick = {})
        .clearAndSetSemantics { contentDescription = "처리 중" },
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.size(42.dp),
      shape = CircleShape,
      color = ChaekAccentSoft,
      border = BorderStroke(1.dp, ChaekBorder),
    ) {
      Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = ChaekAccent,
          strokeWidth = 2.dp,
        )
      }
    }
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
      "SURVIVAL\nSTORIES\n2026",
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
    color = ChaekSurfaceMuted,
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
private fun BookSummary(book: BookDetailArgs, averageRating: Double?) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 18.dp, end = 20.dp, bottom = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      book.title,
      style = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif, fontSize = 32.sp, fontWeight = FontWeight.SemiBold),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
    Text(
      book.creator.ifBlank { "책 정보 준비 중" },
      color = ChaekInkSecondary,
      fontFamily = FontFamily.SansSerif,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
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
      AverageRatingChip(averageRating)
    }
  }
}

@Composable
private fun MetaChip(label: String) {
  Surface(
    modifier = Modifier.padding(horizontal = 4.dp).height(28.dp),
    shape = RoundedCornerShape(999.dp),
    color = ChaekBand,
  ) {
    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
      Text(label, color = ChaekInk, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun AverageRatingChip(averageRating: Double?) {
  Surface(
    modifier = Modifier.padding(start = 4.dp).height(28.dp),
    shape = RoundedCornerShape(4.dp),
    color = ChaekAccentSoft,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("★★★★☆", color = ChaekAccent, fontSize = 15.sp)
      Text(
        averageRating?.let { "%.1f".format(it) } ?: "평점 없음",
        color = ChaekInk,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun ReadingRecord(
  book: BookDetailArgs,
  record: LibraryRecord?,
  onRate: () -> Unit,
  onStatusChange: (ReadingStatus) -> Unit,
  onPageInput: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("내 독서 기록", color = ChaekInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      Spacer(Modifier.weight(1f))
      Surface(
        onClick = onRate,
        shape = RoundedCornerShape(4.dp),
        color = ChaekSurface,
        border = BorderStroke(1.dp, ChaekInk),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(painterResource(R.drawable.ic_star), contentDescription = null, modifier = Modifier.size(12.dp), tint = ChaekInk)
          Text("별점 주기", color = ChaekInk, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      ReadingStatus.entries.forEach { status ->
        val selected = record?.status == status.toApiStatus()
        Surface(
          onClick = { onStatusChange(status) },
          modifier = Modifier.weight(1f).height(32.dp),
          shape = RoundedCornerShape(999.dp),
          color = if (selected) ChaekBand else ChaekSurface,
          contentColor = ChaekInk,
          border = BorderStroke(1.dp, if (selected) ChaekInk else ChaekBorder),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              status.label,
              color = if (selected) ChaekInk else ChaekInkSecondary,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        onClick = onPageInput,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
        color = ChaekSurface,
        border = BorderStroke(1.dp, ChaekBorder),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.spacedBy(7.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(13.dp), tint = ChaekInkSecondary)
          Text("지금 읽는 쪽", color = ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
          Text("${record?.currentPage ?: 0}", color = ChaekInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text(
            if (book.totalPages > 0) "/ ${book.totalPages}쪽" else "/ 쪽수 미정",
            color = ChaekInkSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
          )
        }
      }
      Surface(
        onClick = onPageInput,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(6.dp),
        color = ChaekInk,
        border = BorderStroke(1.dp, ChaekSurface),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(painterResource(R.drawable.ic_pencil), contentDescription = null, modifier = Modifier.size(14.dp), tint = ChaekSurface)
          Text("쪽수 입력", color = ChaekSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
    Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFEDE6DC))) {
      Box(
        Modifier
          .fillMaxWidth(if (book.totalPages > 0) (record?.currentPage ?: 0).toFloat().div(book.totalPages).coerceIn(0f, 1f) else 0f)
          .height(5.dp)
          .background(ChaekInk),
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
  onLike: (Long) -> Unit,
  onReply: (Long, String) -> Unit,
) {
  var replyTarget by remember { mutableStateOf<BookReview?>(null) }
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("감상 $reviewCount", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        onClick = { onSortChange(if (sort == ReviewSort.LATEST) ReviewSort.PAGE else ReviewSort.LATEST) },
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(999.dp),
        color = ChaekBand,
        border = BorderStroke(1.dp, ChaekInk),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            if (sort == ReviewSort.LATEST) "최신순" else "페이지순",
            color = ChaekInk,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
          )
          Icon(
            painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            modifier = Modifier.size(11.dp),
            tint = ChaekInk,
          )
        }
      }
      Surface(shape = RoundedCornerShape(999.dp), color = Color.Transparent, border = BorderStroke(1.dp, ChaekBorder)) {
        Row(modifier = Modifier.padding(2.dp)) {
          Box(
            modifier = Modifier.height(24.dp).clip(RoundedCornerShape(999.dp)).background(if (scope == ReviewScope.ALL) ChaekInk else Color.Transparent).clickable { onScopeChange(ReviewScope.ALL) }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("전체 피드", color = if (scope == ReviewScope.ALL) ChaekSurface else ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = if (scope == ReviewScope.ALL) FontWeight.SemiBold else FontWeight.Normal)
          }
          Box(
            modifier = Modifier.height(24.dp).clip(RoundedCornerShape(999.dp)).background(if (scope == ReviewScope.MINE) ChaekInk else Color.Transparent).clickable { onScopeChange(ReviewScope.MINE) }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text("내 피드", color = if (scope == ReviewScope.MINE) ChaekSurface else ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = if (scope == ReviewScope.MINE) FontWeight.SemiBold else FontWeight.Normal)
          }
        }
      }
    }
    when {
      loading -> Text("감상을 불러오는 중이에요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
      reviews.isEmpty() -> Text("아직 등록된 감상이 없어요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
      else -> reviews.forEach { ReviewCard(it, onLike, onReply = { replyTarget = it }) }
    }
  }
  replyTarget?.let { review ->
    ReplyInputSheet(
      onDismiss = { replyTarget = null },
      onSave = { content ->
        onReply(review.reviewId, content)
        replyTarget = null
      },
    )
  }
}

@Composable
private fun ReviewCard(review: BookReview, onLike: (Long) -> Unit, onReply: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth().background(ChaekBackground).padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(modifier = Modifier.size(34.dp), shape = CircleShape, color = ChaekAccentSoft) {
        Box(contentAlignment = Alignment.Center) { Text(review.authorName.take(1)) }
      }
      Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
        Text(if (review.anonymous) "${review.authorName} (익명)" else review.authorName, style = MaterialTheme.typography.titleSmall)
        Text(
          "${review.createdAt.take(10).replace('-', '.')} · ${review.currentPage?.let { "p.${it}까지" }.orEmpty()}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
    Text(review.content, fontSize = 12.5.sp, lineHeight = 21.sp)
    review.quote?.let { quote ->
      Text("“$quote”", modifier = Modifier.fillMaxWidth().background(ChaekAccentSoft).padding(horizontal = 14.dp, vertical = 12.dp), fontSize = 12.sp, lineHeight = 19.sp)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Spacer(Modifier.weight(1f))
      Text("♡ 좋아요 ${review.likeCount}", modifier = Modifier.clickable { onLike(review.reviewId) }, style = MaterialTheme.typography.labelSmall)
      Row(modifier = Modifier.clickable(role = Role.Button, onClick = onReply), verticalAlignment = Alignment.CenterVertically) {
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
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color.White,
  ) {
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PageInputSheet(initialPage: Int, totalPages: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
  var value by rememberSaveable { mutableStateOf(initialPage.toString()) }
  val page = value.toIntOrNull()
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("현재 쪽수", style = MaterialTheme.typography.titleLarge)
      OutlinedTextField(
        value = value,
        onValueChange = { value = it.filter(Char::isDigit) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        label = { Text(if (totalPages > 0) "1 - ${totalPages}쪽" else "쪽수") },
      )
      Surface(
        onClick = { page?.takeIf { it >= 0 && (totalPages == 0 || it <= totalPages) }?.let(onSave) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ChaekInk,
      ) {
        Text("저장", modifier = Modifier.padding(vertical = 14.dp), color = ChaekSurface, textAlign = TextAlign.Center)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReviewInputSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
  var content by rememberSaveable { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("감상 남기기", style = MaterialTheme.typography.titleLarge)
      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("이 책의 감상을 적어 주세요") },
        minLines = 4,
      )
      Surface(
        onClick = { content.trim().takeIf(String::isNotEmpty)?.let(onSave) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ChaekInk,
      ) {
        Text("등록", modifier = Modifier.padding(vertical = 14.dp), color = ChaekSurface, textAlign = TextAlign.Center)
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReplyInputSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
  var content by rememberSaveable { mutableStateOf("") }
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("답글 작성", style = MaterialTheme.typography.titleLarge)
      OutlinedTextField(
        value = content,
        onValueChange = { content = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("답글을 입력하세요") },
        minLines = 3,
      )
      Surface(
        onClick = { content.trim().takeIf(String::isNotEmpty)?.let(onSave) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ChaekInk,
      ) {
        Text("등록", modifier = Modifier.padding(vertical = 14.dp), color = ChaekSurface, textAlign = TextAlign.Center)
      }
    }
  }
}

private fun ReadingStatus.toApiStatus() =
  when (this) {
    ReadingStatus.WantToRead -> "WANT_TO_READ"
    ReadingStatus.Reading -> "READING"
    ReadingStatus.Finished -> "FINISHED"
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
    modifier = modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)).clickable(role = Role.Button, onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    color = ChaekSurface,
    border = BorderStroke(1.dp, ChaekBorder),
  ) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(6.dp), color = ChaekSurfaceMuted) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Text("✎", color = ChaekInkSecondary, fontSize = 14.sp)
          Text(
            "이 순간의 감상 남기기",
            modifier = Modifier.padding(start = 8.dp),
            color = ChaekInkSecondary,
            fontSize = 11.sp,
          )
        }
      }
      Text("➤", modifier = Modifier.padding(horizontal = 10.dp), color = ChaekInk)
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
