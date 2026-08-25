package com.chamsae.chaekchaek.ui.bookdetail

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.chamsae.chaekchaek.R
import com.chamsae.chaekchaek.auth.AuthSession
import com.chamsae.chaekchaek.auth.requestGoogleIdToken
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.BookRatingStore
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.RatedBook
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
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewReply
import com.chaekchaek.app.data.remote.ReplyPage
import com.chaekchaek.app.domain.rating.Rating
import kotlinx.coroutines.launch

@Composable
fun BookDetailRoute(
  book: BookDetailArgs,
  bookDetailRepository: BookDetailRemoteRepository,
  bookRatingStore: BookRatingStore,
  mobileAuthRepository: MobileAuthRemoteRepository,
  authSession: AuthSession,
  libraryRepository: LibraryRepository,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val tokens by authSession.tokens.collectAsStateWithLifecycle()
  val archivedBooks by libraryRepository.items.collectAsStateWithLifecycle()
  val memberId by libraryRepository.memberId.collectAsStateWithLifecycle()
  val anonymousReviews by libraryRepository.anonymousReviews.collectAsStateWithLifecycle()
  val nickname by libraryRepository.nickname.collectAsStateWithLifecycle()
  val ratings by bookRatingStore.ratings.collectAsStateWithLifecycle()
  var detail by remember(book.isbn13) { mutableStateOf<BookDetail?>(null) }
  var reviews by remember(book.isbn13) { mutableStateOf(emptyList<BookReview>()) }
  var reviewCount by remember(book.isbn13) { mutableStateOf(0) }
  var reviewsLoading by remember(book.isbn13) { mutableStateOf(false) }
  var nextReviewsLoading by remember(book.isbn13) { mutableStateOf(false) }
  var nextReviewPage by remember(book.isbn13) { mutableStateOf<Int?>(null) }
  var reviewScope by rememberSaveable(book.isbn13) { mutableStateOf(ReviewScope.ALL) }
  var reviewSort by rememberSaveable(book.isbn13) { mutableStateOf(ReviewSort.LATEST) }
  var reloadNonce by remember { mutableStateOf(0) }
  val archivedBook = archivedBooks.firstOrNull { it.id == book.isbn13.ifBlank { book.id } }
  val displayBook = detail?.toBookDetailArgs(book) ?: archivedBook?.toBookDetailArgs() ?: book
  val ratingBookId = displayBook.isbn13.ifBlank { displayBook.id }

  suspend fun bookIdForWrite(): Long =
    detail?.bookId ?: requireNotNull(libraryRepository.add(displayBook.toArchivedBook()))

  LaunchedEffect(book.isbn13, archivedBook?.bookId, tokens?.accessToken, reloadNonce) {
    detail = book.isbn13.takeIf(String::isNotBlank)?.let { runCatching { bookDetailRepository.detail(it, tokens?.accessToken) }.getOrNull() }
  }
  LaunchedEffect(memberId) {
    bookRatingStore.selectAccount(memberId)
  }
  LaunchedEffect(detail?.bookId, reviewScope, reviewSort, tokens?.accessToken, reloadNonce) {
    val bookId = detail?.bookId ?: return@LaunchedEffect
    nextReviewPage = null
    runCatching {
      withDelayedApiLoading({ reviewsLoading = it }) {
        bookDetailRepository.reviews(bookId, reviewScope, reviewSort, tokens?.accessToken)
      }
    }
      .onSuccess {
        reviewCount = it.totalCount
        reviews = it.items
        nextReviewPage = it.nextPage
      }.onFailure {
        reviewCount = 0
        reviews = emptyList()
      }
  }
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
    recentRatings = ratings.sortedBy(RatedBook::ratedAt).takeLast(3),
    onBack = onBack,
    onReviewScopeChange = { reviewScope = it },
    onReviewSortChange = { reviewSort = it },
    nextReviewPage = nextReviewPage,
    onLoadMoreReviews = loadMore@{
      val bookId = detail?.bookId ?: return@loadMore
      val page = nextReviewPage ?: return@loadMore
      if (nextReviewsLoading) return@loadMore
      val requestedScope = reviewScope
      val requestedSort = reviewSort
      nextReviewsLoading = true
      try {
        runCatching {
          bookDetailRepository.reviews(bookId, requestedScope, requestedSort, tokens?.accessToken, page)
        }.onSuccess {
          if (nextReviewPage != page || reviewScope != requestedScope || reviewSort != requestedSort) return@onSuccess
          reviewCount = it.totalCount
          reviews = (reviews + it.items).distinctBy(BookReview::reviewId)
          nextReviewPage = it.nextPage
        }
      } finally {
        nextReviewsLoading = false
      }
    },
    signedIn = tokens != null,
    anonymousReviews = anonymousReviews,
    nickname = nickname,
    onGoogleSignIn = { idToken -> authSession.signIn(mobileAuthRepository.loginWithGoogle(idToken)) },
    onToggleLibrary = {
      if (archivedBook == null) libraryRepository.add(displayBook.toArchivedBook())
      else libraryRepository.remove(setOf(archivedBook.id))
    },
    onStatusChange = { status ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.updateReadingStatus(bookIdForWrite(), status.toApiStatus(), accessToken)
      libraryRepository.changeStatus(setOf(displayBook.isbn13.ifBlank { displayBook.id }), status)
      reloadNonce++
    },
    onPageSave = { page ->
      val accessToken = requireNotNull(tokens).accessToken
      val libraryBookId =
        archivedBook?.bookId
          ?: detail?.bookId?.takeIf { detail?.myRecord != null }
          ?: requireNotNull(libraryRepository.add(displayBook.toArchivedBook()))
      bookDetailRepository.updateCurrentPage(libraryBookId, page, detail?.totalPages, accessToken)
      reloadNonce++
    },
    onRatingSave = { rating ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.rate(bookIdForWrite(), rating.score.toDouble(), accessToken)
      memberId?.let { bookRatingStore.rate(it, ratingBookId, displayBook.title, rating) }
      reloadNonce++
    },
    onReviewCreate = { request ->
      val accessToken = requireNotNull(tokens).accessToken
      bookDetailRepository.createReview(bookIdForWrite(), request, accessToken)
      reloadNonce++
    },
    onReviewLike = { reviewId, likedByMe ->
      val accessToken = requireNotNull(tokens).accessToken
      if (likedByMe) bookDetailRepository.unlikeReview(reviewId, accessToken)
      else bookDetailRepository.likeReview(reviewId, accessToken)
      reloadNonce++
    },
    onLoadReplies = { reviewId ->
      val allReplies = loadAllReplies { page ->
        bookDetailRepository.replies(reviewId, tokens?.accessToken, page)
      }
      reviews = reviews.map { review ->
        if (review.reviewId == reviewId) review.copy(recentReplies = allReplies) else review
      }
    },
    onReplyCreate = { reviewId, content ->
      bookDetailRepository.createReply(reviewId, content, requireNotNull(tokens).accessToken)
      reloadNonce++
    },
    onReplyLike = { replyId, likedByMe ->
      val accessToken = requireNotNull(tokens).accessToken
      if (likedByMe) bookDetailRepository.unlikeReply(replyId, accessToken)
      else bookDetailRepository.likeReply(replyId, accessToken)
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
  recentRatings: List<RatedBook> = emptyList(),
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  onReviewScopeChange: (ReviewScope) -> Unit = {},
  onReviewSortChange: (ReviewSort) -> Unit = {},
  nextReviewPage: Int? = null,
  onLoadMoreReviews: suspend () -> Unit = {},
  signedIn: Boolean = false,
  anonymousReviews: Boolean = true,
  nickname: String = "",
  onGoogleSignIn: suspend (String) -> Unit = {},
  onToggleLibrary: suspend () -> Unit = {},
  onStatusChange: suspend (ReadingStatus) -> Unit = {},
  onPageSave: suspend (Int) -> Unit = {},
  onRatingSave: suspend (Rating) -> Unit = {},
  onReviewCreate: suspend (ReviewCreateRequest) -> Unit = {},
  onReviewLike: suspend (Long, Boolean) -> Unit = { _, _ -> },
  onLoadReplies: suspend (Long) -> Unit = {},
  onReplyCreate: suspend (Long, String) -> Unit = { _, _ -> },
  onReplyLike: suspend (Long, Boolean) -> Unit = { _, _ -> },
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
  val reachedBottom by remember { derivedStateOf { !listState.canScrollForward } }
  var showLoginSheet by rememberSaveable { mutableStateOf(false) }
  val context = LocalContext.current
  var signingIn by rememberSaveable { mutableStateOf(false) }
  var loginError by rememberSaveable { mutableStateOf<String?>(null) }
  var requestInFlight by remember { mutableStateOf(false) }
  var showRequestLoading by remember { mutableStateOf(false) }
  var showRatingDialog by rememberSaveable { mutableStateOf(false) }
  var showPageDialog by rememberSaveable { mutableStateOf(false) }
  var showReviewSheet by rememberSaveable { mutableStateOf(false) }
  var pendingSpoilerPage by rememberSaveable(book.id) { mutableStateOf<Int?>(null) }
  var spoilersRevealed by rememberSaveable(book.id) { mutableStateOf(false) }
  val currentPage = myRecord?.currentPage ?: archivedBook?.currentPage ?: 0
  val snackbarHostState = remember { SnackbarHostState() }
  var pendingAfterLogin by remember { mutableStateOf<(() -> Unit)?>(null) }
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
  fun requireLogin(afterLogin: () -> Unit) {
    pendingAfterLogin = afterLogin
    showLoginSheet = true
  }
  fun runAuthenticated(operation: String, action: suspend () -> Unit) {
    if (!signedIn) {
      requireLogin { execute(operation, action) }
      return
    }
    execute(operation, action)
  }
  LaunchedEffect(reachedBottom, nextReviewPage, reviewScope, reviewSort) {
    if (reachedBottom && nextReviewPage != null) onLoadMoreReviews()
  }

  Box(
    modifier = modifier.fillMaxSize().background(ChaekBackground).navigationBarsPadding(),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      state = listState,
      contentPadding = PaddingValues(bottom = 82.dp),
    ) {
      item {
        ArchiveStage(book, archivedBook != null, onBack) {
          runAuthenticated(if (archivedBook == null) "보관" else "보관 해제") { onToggleLibrary() }
        }
      }
      item { BookSummary(book, averageRating) }
      item {
        ReadingRecord(
          book = book,
          record = myRecord,
          currentPage = currentPage,
          onRate = { if (signedIn) showRatingDialog = true else requireLogin { showRatingDialog = true } },
          onStatusChange = { status -> runAuthenticated("독서 상태 변경") { onStatusChange(status) } },
          onPageInput = { if (signedIn) showPageDialog = true else requireLogin { showPageDialog = true } },
        )
      }
      item { Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand)) }
      item {
        ReviewsSection(
          modifier = Modifier.fillParentMaxHeight(),
          reviews = reviews,
          reviewCount = reviewCount,
          loading = reviewsLoading,
          currentPage = currentPage,
          spoilersRevealed = spoilersRevealed,
          scope = reviewScope,
          sort = reviewSort,
          signedIn = signedIn,
          onScopeChange = { requested ->
            if (requested == ReviewScope.MINE && !signedIn) {
              requireLogin { onReviewScopeChange(ReviewScope.MINE) }
            } else {
              onReviewScopeChange(requested)
            }
          },
          onSortChange = onReviewSortChange,
          onLoginRequired = { afterLogin -> requireLogin(afterLogin) },
          onOpenLockedReview = { pendingSpoilerPage = it },
          onLike = { reviewId, likedByMe -> runAuthenticated("감상 반응") { onReviewLike(reviewId, likedByMe) } },
          onLoadReplies = { reviewId -> execute("답글 조회") { onLoadReplies(reviewId) } },
          onReply = { reviewId, content -> runAuthenticated("답글 작성") { onReplyCreate(reviewId, content) } },
          onReplyLike = { replyId, likedByMe -> runAuthenticated("답글 반응") { onReplyLike(replyId, likedByMe) } },
        )
      }
    }

    ComposeBar(
      onClick = { if (signedIn) showReviewSheet = true else requireLogin { showReviewSheet = true } },
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
      onDismiss = {
        if (!signingIn) {
          showLoginSheet = false
          pendingAfterLogin = null
        }
      },
      onGoogleSignIn = {
        if (signingIn) return@LoginRequiredSheet
        scope.launch {
          signingIn = true
          loginError = null
          runCatching { requestGoogleIdToken(context) }
            .mapCatching { idToken -> onGoogleSignIn(idToken) }
            .onSuccess {
              val afterLogin = pendingAfterLogin
              pendingAfterLogin = null
              showLoginSheet = false
              afterLogin?.invoke()
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
      currentBookId = book.id,
      initialRating = myRecord?.rating?.let { Rating.ofScore(it.toFloat()) },
      recentRatings = recentRatings,
      onDismiss = { showRatingDialog = false },
      onSave = { rating ->
        runAuthenticated("별점 저장") { onRatingSave(rating) }
        showRatingDialog = false
      },
    )
  }
  if (showPageDialog) {
    PageInputDialog(
      initialPage = currentPage,
      totalPages = book.totalPages,
      onDismiss = { showPageDialog = false },
      onSave = { page ->
        runAuthenticated("쪽수 저장") { onPageSave(page) }
        showPageDialog = false
      },
    )
  }
  pendingSpoilerPage?.let { spoilerPage ->
    PageInputDialog(
      initialPage = currentPage,
      totalPages = book.totalPages,
      spoilerPage = spoilerPage,
      onDismiss = { pendingSpoilerPage = null },
      onSave = { page ->
        pendingSpoilerPage = null
        runAuthenticated("쪽수 저장") { onPageSave(page) }
      },
      onReadAnyway = {
        spoilersRevealed = true
        pendingSpoilerPage = null
      },
    )
  }
  if (showReviewSheet) {
    ReviewInputSheet(
      initialPage = currentPage,
      totalPages = book.totalPages,
      anonymous = anonymousReviews,
      nickname = nickname,
      onDismiss = { showReviewSheet = false },
      onSave = { request ->
        runAuthenticated("감상 작성") { onReviewCreate(request) }
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
private fun ArchiveStage(book: BookDetailArgs, saved: Boolean, onBack: () -> Unit, onLibraryClick: () -> Unit) {
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
      color = if (saved) ChaekAccent else Color.Transparent,
      border = if (saved) null else BorderStroke(1.dp, ChaekSurface),
    ) {
      Box(
        modifier = Modifier.semantics { contentDescription = if (saved) "서재에서 삭제" else "서재에 추가" },
        contentAlignment = Alignment.Center,
      ) {
        Text("⌑", color = if (saved) ChaekInk else ChaekSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
@OptIn(ExperimentalLayoutApi::class)
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
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalArrangement = Arrangement.spacedBy(8.dp),
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
    modifier = Modifier.height(28.dp),
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
      AverageRatingStars(averageRating)
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
private fun AverageRatingStars(averageRating: Double?) {
  Row {
    averageRatingStarFillFractions(averageRating).forEach { fillFraction ->
      Box(modifier = Modifier.size(width = 15.dp, height = 20.dp), contentAlignment = Alignment.Center) {
        Text("★", color = ChaekBorder, fontSize = 15.sp)
        if (fillFraction > 0f) {
          Box(
            modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(fillFraction).clipToBounds(),
            contentAlignment = Alignment.CenterStart,
          ) {
            Text("★", modifier = Modifier.width(15.dp), color = ChaekAccent, fontSize = 15.sp)
          }
        }
      }
    }
  }
}

internal fun averageRatingStarFillFractions(averageRating: Double?): List<Float> =
  List(5) { starIndex -> ((averageRating ?: 0.0) - starIndex).coerceIn(0.0, 1.0).toFloat() }

@Composable
private fun ReadingRecord(
  book: BookDetailArgs,
  record: LibraryRecord?,
  currentPage: Int,
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
          Text("$currentPage", color = ChaekInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
          .fillMaxWidth(if (book.totalPages > 0) currentPage.toFloat().div(book.totalPages).coerceIn(0f, 1f) else 0f)
          .height(5.dp)
          .background(ChaekInk),
      )
    }
  }
}

@Composable
private fun ReviewsSection(
  modifier: Modifier = Modifier,
  reviews: List<BookReview>,
  reviewCount: Int,
  loading: Boolean,
  currentPage: Int,
  spoilersRevealed: Boolean,
  scope: ReviewScope,
  sort: ReviewSort,
  signedIn: Boolean,
  onScopeChange: (ReviewScope) -> Unit,
  onSortChange: (ReviewSort) -> Unit,
  onLoginRequired: (() -> Unit) -> Unit,
  onOpenLockedReview: (Int) -> Unit,
  onLike: (Long, Boolean) -> Unit,
  onLoadReplies: (Long) -> Unit,
  onReply: (Long, String) -> Unit,
  onReplyLike: (Long, Boolean) -> Unit,
) {
  var replyTarget by remember { mutableStateOf<BookReview?>(null) }
  var sortExpanded by remember { mutableStateOf(false) }
  Column(modifier = modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
      Text("감상 $reviewCount", style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif))
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box {
        Surface(
          onClick = { sortExpanded = true },
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
              sort.label,
              color = ChaekInk,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
            )
            Icon(
              painterResource(R.drawable.ic_chevron_down),
              contentDescription = "감상 정렬 선택",
              modifier = Modifier.size(11.dp),
              tint = ChaekInk,
            )
          }
        }
        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
          listOf(ReviewSort.LATEST, ReviewSort.PAGE).forEach { option ->
            DropdownMenuItem(
              text = { Text(option.label) },
              onClick = {
                sortExpanded = false
                onSortChange(option)
              },
            )
          }
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
      loading && reviews.isEmpty() -> Text("감상을 불러오는 중이에요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
      reviews.isEmpty() -> Text("아직 등록된 감상이 없어요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
      else -> reviews.forEach { review ->
        val locked = shouldLockReview(currentPage, review.currentPage, spoilersRevealed)
        ReviewCard(
          review = review,
          locked = locked,
          onOpenLockedReview = { review.currentPage?.let(onOpenLockedReview) },
          onLike = onLike,
          onLoadReplies = { onLoadReplies(review.reviewId) },
          onReply = {
            if (signedIn) replyTarget = review
            else onLoginRequired { replyTarget = review }
          },
          onReplyLike = onReplyLike,
        )
      }
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

private val ReviewSort.label: String
  get() =
    when (this) {
      ReviewSort.LATEST -> "최신순"
      ReviewSort.PAGE -> "페이지순"
      ReviewSort.OLDEST -> "오래된순"
      ReviewSort.POPULAR -> "인기순"
    }

@Composable
private fun ReviewCard(
  review: BookReview,
  locked: Boolean,
  onOpenLockedReview: () -> Unit,
  onLike: (Long, Boolean) -> Unit,
  onLoadReplies: () -> Unit,
  onReply: () -> Unit,
  onReplyLike: (Long, Boolean) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().background(ChaekBackground).padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(review.authorName, review.authorProfileImageUrl, 34)
        Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
          Text(review.authorName, style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
              review.createdAt.take(10).replace('-', '.'),
              color = ChaekInkSecondary,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
            )
            review.currentPage?.let { page ->
              Surface(shape = RoundedCornerShape(999.dp), color = ChaekAccentSoft) {
                Text(
                  "p.${page}까지",
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  color = ChaekAccentInk,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }
      Text(
        if (locked) maskAsChirps(review.content) else review.content,
        modifier =
          if (locked) {
            Modifier.clickable(role = Role.Button, onClick = onOpenLockedReview)
              .clearAndSetSemantics { contentDescription = "감상 내용 잠김" }
          } else {
            Modifier
          },
        fontSize = 12.5.sp,
        lineHeight = 21.sp,
      )
      review.quote?.let { quote ->
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min)
              .background(ChaekAccentSoft)
              .then(
                if (locked) {
                  Modifier.clickable(role = Role.Button, onClick = onOpenLockedReview)
                    .clearAndSetSemantics { contentDescription = "인용문 잠김" }
                } else {
                  Modifier
                },
              ),
        ) {
          Box(Modifier.width(2.dp).fillMaxHeight().background(ChaekInk))
          Text(
            "“${if (locked) maskAsChirps(quote) else quote}”",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            fontSize = 12.sp,
            lineHeight = 19.sp,
          )
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        ReviewAction(
          icon = if (review.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline,
          label = "좋아요 ${review.likeCount}",
          description = if (review.likedByMe) "감상 좋아요 취소" else "감상 좋아요",
          onClick = { onLike(review.reviewId, review.likedByMe) },
        )
        ReviewAction(
          icon = R.drawable.ic_comment,
          label = "답글 ${review.replyCount}",
          description = if (locked) "잠긴 감상의 답글 확인" else "감상에 답글 작성",
          onClick = if (locked) onOpenLockedReview else onReply,
        )
      }
    }
    if (review.replyCount > 0) {
      Replies(
        replies = review.recentReplies,
        totalCount = review.replyCount,
        locked = locked,
        onOpenLockedReview = onOpenLockedReview,
        onLoadAll = if (locked) onOpenLockedReview else onLoadReplies,
        onLike = onReplyLike,
      )
    }
  }
}

@Composable
private fun Replies(
  replies: List<ReviewReply>,
  totalCount: Int,
  locked: Boolean,
  onOpenLockedReview: () -> Unit,
  onLoadAll: () -> Unit,
  onLike: (Long, Boolean) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().background(ChaekSurfaceMuted).padding(start = 48.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    replies.forEach { reply ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        AuthorAvatar(reply.authorName, reply.authorProfileImageUrl, 24)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Text(reply.authorName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          Text(
            if (locked) maskAsChirps(reply.content) else reply.content,
            modifier =
              if (locked) {
                Modifier.clickable(role = Role.Button, onClick = onOpenLockedReview)
                  .clearAndSetSemantics { contentDescription = "답글 내용 잠김" }
              } else {
                Modifier
              },
            color = ChaekInkSecondary,
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
          )
        }
        Column(
          modifier =
            Modifier
              .clickable(role = Role.Button) { onLike(reply.replyId, reply.likedByMe) }
              .clearAndSetSemantics { contentDescription = if (reply.likedByMe) "답글 좋아요 취소" else "답글 좋아요" },
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            painterResource(if (reply.likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline),
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = ChaekInkSecondary,
          )
          Text("${reply.likeCount}", color = ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp)
        }
      }
    }
    if (replies.size < totalCount) {
      Text(
        "답글 ${totalCount}개 모두 보기",
        modifier = Modifier.clickable(role = Role.Button, onClick = onLoadAll).padding(vertical = 4.dp),
        color = ChaekInk,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
private fun AuthorAvatar(name: String, profileImageUrl: String?, size: Int) {
  val fallbackAvatar = painterResource(authorAvatarResource(name))
  AsyncImage(
    model = profileImageUrl,
    contentDescription = null,
    modifier = Modifier.size(size.dp).clip(CircleShape),
    placeholder = fallbackAvatar,
    error = fallbackAvatar,
    fallback = fallbackAvatar,
    contentScale = ContentScale.Crop,
  )
}

internal fun authorAvatarResource(displayName: String): Int =
  when (Math.floorMod(displayName.hashCode(), 4)) {
    0 -> R.drawable.avatar_reading
    1 -> R.drawable.avatar_kim
    2 -> R.drawable.avatar_yoon
    else -> R.drawable.avatar_tea
  }

@Composable
private fun ReviewAction(
  icon: Int,
  label: String,
  description: String,
  onClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .clickable(role = Role.Button, onClick = onClick)
        .clearAndSetSemantics { contentDescription = description }
        .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp), tint = ChaekInk)
    Text(label, color = ChaekInk, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
  }
}

internal suspend fun loadAllReplies(loadPage: suspend (Int) -> ReplyPage): List<ReviewReply> {
  val replies = mutableListOf<ReviewReply>()
  val loadedPages = mutableSetOf<Int>()
  var page: Int? = 1
  while (page != null && loadedPages.add(page)) {
    val response = loadPage(page)
    replies += response.items
    page = response.nextPage
  }
  return replies.distinctBy(ReviewReply::replyId)
}

internal fun shouldLockReview(
  currentPage: Int,
  reviewPage: Int?,
  spoilersRevealed: Boolean,
): Boolean = !spoilersRevealed && reviewPage?.let { it > currentPage } == true

internal fun maskAsChirps(content: String): String =
  content.map { character -> if (character.isWhitespace() || character in VISIBLE_MASK_PUNCTUATION) character else '짹' }.joinToString("")

private const val VISIBLE_MASK_PUNCTUATION = ".,!?…:;\"'“”‘’()[]{}-·"

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LoginRequiredSheet(
  signingIn: Boolean,
  error: String?,
  onDismiss: () -> Unit,
  onGoogleSignIn: () -> Unit,
) {
  val uriHandler = LocalUriHandler.current
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
      Text(
        "개인정보처리방침",
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { uriHandler.openUri(PRIVACY_POLICY_URL) }
            .padding(vertical = 8.dp),
        color = ChaekInkSecondary,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun PageInputDialog(
  initialPage: Int,
  totalPages: Int,
  onDismiss: () -> Unit,
  onSave: (Int) -> Unit,
  spoilerPage: Int? = null,
  onReadAnyway: (() -> Unit)? = null,
) {
  var value by rememberSaveable { mutableStateOf(initialPage.toString()) }
  val page = BookDetailInputRules.validPage(value, totalPages)
  val spoilerMode = onReadAnyway != null
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).widthIn(max = 330.dp).shadow(16.dp, RoundedCornerShape(12.dp)),
      shape = RoundedCornerShape(12.dp),
      color = ChaekSurface,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        SheetHeader(title = "어디까지 읽으셨나요?", titleSize = 16, onDismiss = onDismiss)
        Text(
          spoilerPage?.let { "이 감상은 ${it}쪽 이후 내용을 포함해요. 내가 읽은 쪽수를 입력하면 읽은 범위까지 안전하게 볼 수 있어요." }
            ?: if (spoilerMode) "이 감상은 스포일러를 포함해요. 내가 읽은 쪽수를 입력하거나 스포일러를 감수하고 볼 수 있어요."
            else "지금까지 읽은 쪽수를 입력하면 독서 진행률과 감상 열람 범위에 반영돼요.",
          modifier = Modifier.fillMaxWidth(),
          color = ChaekInkSecondary,
          fontSize = 11.5.sp,
          lineHeight = 18.sp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FormLabel("내가 읽은 쪽수")
          ChaekTextInput(
            value = value,
            onValueChange = { value = it.filter(Char::isDigit).take(7) },
            placeholder = "0",
            accessibilityLabel = "내가 읽은 쪽수",
            modifier = Modifier.fillMaxWidth(),
            height = 44,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = "쪽",
            endText = totalPages.takeIf { it > 0 }?.let { "/ ${it}쪽" },
            emphasized = true,
          )
        }
        SheetPrimaryButton(
          label = if (spoilerMode) "입력한 쪽수까지 보기" else "읽은 쪽수 저장",
          enabled = page != null && (spoilerPage == null || page >= spoilerPage),
        ) {
          page?.let(onSave)
        }
        onReadAnyway?.let { readAnyway ->
          SheetPrimaryButton(label = "스포일러 감수하고 보기", enabled = true, danger = true, onClick = readAnyway)
        }
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReviewInputSheet(
  initialPage: Int,
  totalPages: Int,
  anonymous: Boolean,
  nickname: String,
  onDismiss: () -> Unit,
  onSave: (ReviewCreateRequest) -> Unit,
) {
  var content by rememberSaveable { mutableStateOf("") }
  var quote by rememberSaveable { mutableStateOf("") }
  var chapter by rememberSaveable { mutableStateOf("") }
  var pageValue by rememberSaveable { mutableStateOf(initialPage.takeIf { it > 0 }?.toString().orEmpty()) }
  var isSpoiler by rememberSaveable { mutableStateOf(false) }
  var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
  val page = BookDetailInputRules.validPage(pageValue, totalPages)
  val canSubmit = BookDetailInputRules.canSubmitReview(content, pageValue, totalPages)
  val hasDraft = BookDetailInputRules.hasReviewDraft(content, quote, chapter, pageValue, initialPage, isSpoiler)
  val requestDismiss = { if (hasDraft) showDiscardConfirmation = true else onDismiss() }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = requestDismiss,
    sheetState = sheetState,
    containerColor = ChaekSurface,
    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    dragHandle = {
      Box(
        Modifier.padding(top = 10.dp).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFDDDDDD)),
      )
    },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      SheetHeader(title = "감상 남기기", titleSize = 20, onDismiss = requestDismiss)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel("느낀점", required = true)
        ChaekTextInput(
          value = content,
          onValueChange = { content = it.take(BookDetailInputRules.MAX_CONTENT_LENGTH) },
          placeholder = "이 구간을 읽으며 든 생각을 남겨보세요",
          accessibilityLabel = "느낀점",
          modifier = Modifier.fillMaxWidth(),
          height = 132,
        )
        Text(
          "${content.length} / ${BookDetailInputRules.MAX_CONTENT_LENGTH}",
          modifier = Modifier.fillMaxWidth(),
          color = ChaekInkSecondary,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          textAlign = TextAlign.End,
        )
        Surface(
          modifier = Modifier.fillMaxWidth().height(40.dp).toggleable(value = isSpoiler, role = Role.Checkbox) { isSpoiler = it },
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFFF6F2EC),
        ) {
          Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier
                  .size(18.dp)
                  .background(if (isSpoiler) ChaekInk else ChaekSurface, RoundedCornerShape(4.dp))
                  .border(1.dp, if (isSpoiler) ChaekInk else ChaekBorder, RoundedCornerShape(4.dp)),
              contentAlignment = Alignment.Center,
            ) {
              if (isSpoiler) {
                Icon(
                  painter = painterResource(R.drawable.ic_check),
                  contentDescription = null,
                  modifier = Modifier.size(12.dp),
                  tint = ChaekSurface,
                )
              }
            }
            Text("스포일러", modifier = Modifier.padding(start = 8.dp), color = Color(0xFFC92A24), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
          }
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormLabel("인상 깊은 문구")
        ChaekTextInput(
          value = quote,
          onValueChange = { quote = it.take(BookDetailInputRules.MAX_QUOTE_LENGTH) },
          placeholder = "기억하고 싶은 문장을 옮겨 적어보세요",
          accessibilityLabel = "인상 깊은 문구",
          modifier = Modifier.fillMaxWidth(),
          height = 72,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FormLabel("쪽수")
          ChaekTextInput(
            value = pageValue,
            onValueChange = { pageValue = it.filter(Char::isDigit).take(7) },
            placeholder = "80",
            accessibilityLabel = "쪽수",
            modifier = Modifier.fillMaxWidth(),
            height = 44,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = "쪽",
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FormLabel("목차 / 챕터")
          ChaekTextInput(
            value = chapter,
            onValueChange = { chapter = it.take(BookDetailInputRules.MAX_CHAPTER_LENGTH) },
            placeholder = "Chapter 1",
            accessibilityLabel = "목차 또는 챕터",
            modifier = Modifier.fillMaxWidth(),
            height = 44,
            singleLine = true,
          )
        }
      }
      Surface(modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(6.dp), color = Color(0xFFF6F2EC)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(painterResource(R.drawable.ic_eye_off), contentDescription = null, modifier = Modifier.size(14.dp), tint = ChaekInk)
          Text(if (anonymous) "익명" else "공개", modifier = Modifier.padding(start = 7.dp), color = ChaekInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Text(
            if (anonymous) "이름을 숨겨서 표시돼요" else "‘${nickname.ifBlank { "닉네임 없음" }}’으로 표시돼요",
            modifier = Modifier.padding(start = 7.dp),
            color = ChaekInkSecondary,
            fontSize = 11.5.sp,
          )
        }
      }
      SheetPrimaryButton(label = "감상 남기기", enabled = canSubmit) {
        if (!canSubmit) return@SheetPrimaryButton
        onSave(
          ReviewCreateRequest(
            content = content.trim(),
            quote = quote.trim().ifEmpty { null },
            chapter = chapter.trim().ifEmpty { null },
            currentPage = page,
            totalPages = totalPages.takeIf { page != null && it > 0 },
            isSpoiler = isSpoiler,
          ),
        )
      }
    }
  }
  if (showDiscardConfirmation) {
    AlertDialog(
      onDismissRequest = { showDiscardConfirmation = false },
      title = { Text("감상 작성을 그만둘까요?") },
      text = { Text("작성한 내용은 저장되지 않아요.") },
      confirmButton = {
        TextButton(onClick = onDismiss) { Text("작성 취소") }
      },
      dismissButton = {
        TextButton(onClick = { showDiscardConfirmation = false }) { Text("계속 작성") }
      },
    )
  }
}

@Composable
private fun SheetHeader(title: String, titleSize: Int, onDismiss: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(title, color = ChaekInk, fontFamily = FontFamily.Serif, fontSize = titleSize.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.weight(1f))
    Box(
      modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onDismiss),
      contentAlignment = Alignment.Center,
    ) {
      Icon(painterResource(R.drawable.ic_close), contentDescription = "닫기", modifier = Modifier.size(20.dp), tint = ChaekInk)
    }
  }
}

@Composable
private fun FormLabel(label: String, required: Boolean = false) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, color = ChaekInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    if (required) {
      Surface(shape = RoundedCornerShape(8.dp), color = ChaekInk) {
        Text("필수", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = ChaekSurface, fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

@Composable
private fun ChaekTextInput(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  accessibilityLabel: String,
  modifier: Modifier,
  height: Int,
  singleLine: Boolean = false,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  suffix: String? = null,
  endText: String? = null,
  emphasized: Boolean = false,
) {
  val shape = RoundedCornerShape(6.dp)
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier.height(height.dp).background(ChaekSurface, shape).border(if (emphasized) 1.5.dp else 1.dp, if (emphasized) ChaekInk else ChaekBorder, shape).semantics { contentDescription = accessibilityLabel }.padding(horizontal = 12.dp, vertical = if (singleLine || suffix != null || endText != null) 0.dp else 10.dp),
    textStyle = TextStyle(color = ChaekInk, fontFamily = FontFamily.SansSerif, fontSize = 12.5.sp),
    keyboardOptions = keyboardOptions,
    singleLine = singleLine || suffix != null || endText != null,
    decorationBox = { innerTextField ->
      if (singleLine || suffix != null || endText != null) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 11.5.sp)
            innerTextField()
          }
          suffix?.let { Text(it, color = ChaekInk, fontSize = 11.sp) }
          endText?.let { Text(it, modifier = Modifier.padding(start = 6.dp), color = ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp) }
        }
      } else {
        Box(modifier = Modifier.fillMaxSize()) {
          if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 11.5.sp)
          innerTextField()
        }
      }
    },
  )
}

@Composable
private fun SheetPrimaryButton(label: String, enabled: Boolean, danger: Boolean = false, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth().height(48.dp),
    shape = RoundedCornerShape(4.dp),
    color = if (!enabled) ChaekInkSecondary else if (danger) MaterialTheme.colorScheme.error else ChaekInk,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(label, color = ChaekSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
  }
}

internal object BookDetailInputRules {
  const val MAX_CONTENT_LENGTH = 1000
  const val MAX_QUOTE_LENGTH = 500
  const val MAX_CHAPTER_LENGTH = 255

  fun validPage(value: String, totalPages: Int): Int? =
    value.toIntOrNull()?.takeIf { it >= 0 && (totalPages <= 0 || it <= totalPages) }

  fun canSubmitReview(content: String, pageValue: String, totalPages: Int): Boolean =
    content.isNotBlank() && content.length <= MAX_CONTENT_LENGTH &&
      (pageValue.isBlank() || validPage(pageValue, totalPages) != null)

  fun hasReviewDraft(
    content: String,
    quote: String,
    chapter: String,
    pageValue: String,
    initialPage: Int,
    isSpoiler: Boolean,
  ): Boolean =
    content.isNotEmpty() || quote.isNotEmpty() || chapter.isNotEmpty() || isSpoiler ||
      pageValue != initialPage.takeIf { it > 0 }?.toString().orEmpty()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ReplyInputSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
  var content by rememberSaveable { mutableStateOf("") }
  val canSubmit = ReplyInputRules.canSubmit(content)
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("답글 작성", style = MaterialTheme.typography.titleLarge)
      OutlinedTextField(
        value = content,
        onValueChange = { content = it.take(ReplyInputRules.MAX_LENGTH) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("답글을 입력하세요") },
        minLines = 3,
      )
      Text(
        "${content.length} / ${ReplyInputRules.MAX_LENGTH}",
        modifier = Modifier.fillMaxWidth(),
        color = ChaekInkSecondary,
        textAlign = TextAlign.End,
        style = MaterialTheme.typography.bodySmall,
      )
      Surface(
        onClick = { if (canSubmit) onSave(content.trim()) },
        enabled = canSubmit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (canSubmit) ChaekInk else ChaekSurfaceMuted,
      ) {
        Text("등록", modifier = Modifier.padding(vertical = 14.dp), color = ChaekSurface, textAlign = TextAlign.Center)
      }
    }
  }
}

private const val PRIVACY_POLICY_URL =
  "https://app.notion.com/p/3b185850b3e18085b919d108ce7cd4ef?source=copy_link"

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

private fun BookDetailArgs.toArchivedBook() =
  ArchivedBook(
    id = isbn13.ifBlank { id },
    bookId = bookId,
    title = title,
    creator = creator,
    publisher = publisher,
    year = year,
    coverUrl = coverUrl,
    note = "",
    category = category,
    totalPages = totalPages,
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
