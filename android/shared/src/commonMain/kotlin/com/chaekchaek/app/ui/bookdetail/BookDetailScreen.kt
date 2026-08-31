package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.avatar_kim
import chaekchaek.shared.generated.resources.avatar_yoon
import chaekchaek.shared.generated.resources.cover_01
import chaekchaek.shared.generated.resources.cover_02
import chaekchaek.shared.generated.resources.cover_03
import chaekchaek.shared.generated.resources.cover_04
import chaekchaek.shared.generated.resources.cover_05
import chaekchaek.shared.generated.resources.cover_06
import chaekchaek.shared.generated.resources.cover_07
import chaekchaek.shared.generated.resources.cover_08
import chaekchaek.shared.generated.resources.cover_09
import chaekchaek.shared.generated.resources.cover_10
import chaekchaek.shared.generated.resources.cover_11
import chaekchaek.shared.generated.resources.cover_12
import chaekchaek.shared.generated.resources.cover_13
import chaekchaek.shared.generated.resources.cover_14
import chaekchaek.shared.generated.resources.cover_15
import chaekchaek.shared.generated.resources.cover_16
import chaekchaek.shared.generated.resources.cover_17
import chaekchaek.shared.generated.resources.cover_18
import chaekchaek.shared.generated.resources.cover_19
import chaekchaek.shared.generated.resources.cover_20
import chaekchaek.shared.generated.resources.ic_back
import chaekchaek.shared.generated.resources.ic_bookmark
import chaekchaek.shared.generated.resources.ic_chevron_down
import chaekchaek.shared.generated.resources.ic_comment
import chaekchaek.shared.generated.resources.ic_heart_filled
import chaekchaek.shared.generated.resources.ic_heart_outline
import chaekchaek.shared.generated.resources.ic_pencil
import chaekchaek.shared.generated.resources.ic_star
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.LibraryRecord
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.data.remote.ReviewReply
import com.chaekchaek.app.data.remote.ReviewScope
import com.chaekchaek.app.data.remote.ReviewSort
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekAccentSoft
import com.chaekchaek.app.ui.theme.ChaekBackground
import com.chaekchaek.app.ui.theme.ChaekBand
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal val ArchiveStageBackground = Color(0xFF1A1A1A)
private val ArchiveStageForeground = Color.White

@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    recentRatings: List<RatedBookUiModel> = emptyList(),
    savedToLibrary: Boolean = state.detail?.myRecord != null,
    anonymousReviews: Boolean = true,
    nickname: String = "",
    localCurrentPage: Int = 0,
    coverContent: (@Composable (BookDetailArgs, Modifier) -> Unit)? = null,
    resumedAuthenticatedAction: BookDetailAuthenticatedAction? = null,
    onAuthenticatedActionHandled: () -> Unit = {},
    onLoginRequired: (BookDetailAuthenticatedAction) -> Unit = {},
    onToggleLibrary: () -> Unit = {},
    onStatusChange: (ReadingStatus) -> Unit = {},
    onPageSave: (Int) -> Unit = {},
    onRatingSave: (Rating, () -> Unit) -> Unit = { _, _ -> },
    onReviewOpen: (() -> Unit) -> Unit = { it() },
    onReviewCreate: (ReviewCreateRequest) -> Unit = {},
    onReviewUpdate: (Long, ReviewCreateRequest) -> Unit = { _, _ -> },
    onReviewDelete: (Long) -> Unit = {},
    onReviewLike: (Long, Boolean) -> Unit = { _, _ -> },
    onLoadReplies: (Long) -> Unit = {},
    onReplyCreate: (Long, String) -> Unit = { _, _ -> },
    onReplyUpdate: (Long, String) -> Unit = { _, _ -> },
    onReplyDelete: (Long) -> Unit = {},
    onReplyLike: (Long, Boolean) -> Unit = { _, _ -> },
    onReviewScopeChange: (ReviewScope) -> Unit = {},
    onReviewSortChange: (ReviewSort) -> Unit = {},
    onLoadMoreReviews: () -> Unit = {},
    onRequestErrorShown: () -> Unit = {},
) {
    val book = state.displayBook ?: return
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = remember(density) { with(density) { 240.dp.roundToPx() } }
    val showScrollTop by remember(threshold) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset >= threshold }
    }
    val reachedBottom by remember { derivedStateOf { !listState.canScrollForward } }
    val currentPage = state.detail?.myRecord?.currentPage ?: localCurrentPage
    val snackbarHostState = remember { SnackbarHostState() }
    var showRatingDialog by rememberSaveable { mutableStateOf(false) }
    var showPageDialog by rememberSaveable { mutableStateOf(false) }
    var showReviewSheet by rememberSaveable { mutableStateOf(false) }
    var reviewActionTarget by remember { mutableStateOf<BookReview?>(null) }
    var replyActionTarget by remember { mutableStateOf<ReviewReply?>(null) }
    var editingReview by remember { mutableStateOf<BookReview?>(null) }
    var editingReply by remember { mutableStateOf<ReviewReply?>(null) }
    var deletingReview by remember { mutableStateOf<BookReview?>(null) }
    var deletingReply by remember { mutableStateOf<ReviewReply?>(null) }
    var revealedSpoilerReviewIds by remember(book.id) { mutableStateOf(emptySet<Long>()) }

    fun authorizeOrRun(action: BookDetailAuthenticatedAction, onAuthorized: () -> Unit) {
        if (!action.requiresMember || state.signedIn) onAuthorized() else onLoginRequired(action)
    }

    LaunchedEffect(reachedBottom, state.nextReviewPage, state.reviewScope, state.reviewSort) {
        if (reachedBottom && state.nextReviewPage != null) onLoadMoreReviews()
    }
    LaunchedEffect(state.requestError) {
        state.requestError?.let {
            snackbarHostState.showSnackbar(it)
            onRequestErrorShown()
        }
    }
    LaunchedEffect(resumedAuthenticatedAction) {
        when (val action = resumedAuthenticatedAction ?: return@LaunchedEffect) {
            BookDetailAuthenticatedAction.AddToLibrary -> onToggleLibrary()
            BookDetailAuthenticatedAction.OpenPageInput -> showPageDialog = true
            BookDetailAuthenticatedAction.OpenRating -> showRatingDialog = true
            BookDetailAuthenticatedAction.OpenReview -> onReviewOpen { showReviewSheet = true }
            BookDetailAuthenticatedAction.OpenMineFeed -> onReviewScopeChange(ReviewScope.MINE)
            is BookDetailAuthenticatedAction.ChangeStatus -> onStatusChange(action.status)
            is BookDetailAuthenticatedAction.SavePage -> onPageSave(action.page)
            is BookDetailAuthenticatedAction.LikeReview -> onReviewLike(action.reviewId, action.likedByMe)
            is BookDetailAuthenticatedAction.CreateReply -> onReplyCreate(action.reviewId, action.content)
            is BookDetailAuthenticatedAction.LikeReply -> onReplyLike(action.replyId, action.likedByMe)
            is BookDetailAuthenticatedAction.EditReview -> onReviewUpdate(action.reviewId, action.request)
            is BookDetailAuthenticatedAction.DeleteReview -> onReviewDelete(action.reviewId)
            is BookDetailAuthenticatedAction.EditReply -> onReplyUpdate(action.replyId, action.content)
            is BookDetailAuthenticatedAction.DeleteReply -> onReplyDelete(action.replyId)
        }
        onAuthenticatedActionHandled()
    }

    Box(modifier = modifier.fillMaxSize().background(ChaekBackground).navigationBarsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 82.dp),
        ) {
            item {
                ArchiveStage(
                    book = book,
                    saved = savedToLibrary,
                    onBack = onBack,
                    onLibraryClick = {
                        authorizeOrRun(BookDetailAuthenticatedAction.AddToLibrary, onToggleLibrary)
                    },
                    coverContent = coverContent,
                )
            }
            item { BookSummary(book, state.detail?.averageRating) }
            item {
                ReadingRecord(
                    book = book,
                    record = state.detail?.myRecord,
                    currentPage = currentPage,
                    onRate = {
                        authorizeOrRun(BookDetailAuthenticatedAction.OpenRating) { showRatingDialog = true }
                    },
                    onStatusChange = { status ->
                        authorizeOrRun(BookDetailAuthenticatedAction.ChangeStatus(status)) { onStatusChange(status) }
                    },
                    onPageInput = {
                        authorizeOrRun(BookDetailAuthenticatedAction.OpenPageInput) { showPageDialog = true }
                    },
                )
            }
            item { Box(Modifier.fillMaxWidth().height(6.dp).background(ChaekBand)) }
            item {
                ReviewsSection(
                    reviews = state.reviews,
                    reviewCount = state.reviewCount,
                    loading = state.isLoading,
                    loadingMore = state.isLoadingMore,
                    revealedSpoilerReviewIds = revealedSpoilerReviewIds,
                    scope = state.reviewScope,
                    sort = state.reviewSort,
                    onScopeChange = { requested ->
                        if (requested == ReviewScope.MINE && !state.signedIn) {
                            onLoginRequired(BookDetailAuthenticatedAction.OpenMineFeed)
                        } else {
                            onReviewScopeChange(requested)
                        }
                    },
                    onSortChange = onReviewSortChange,
                    onRevealSpoiler = { reviewId ->
                        revealedSpoilerReviewIds += reviewId
                    },
                    onLike = { reviewId, likedByMe ->
                        authorizeOrRun(BookDetailAuthenticatedAction.LikeReview(reviewId, likedByMe)) {
                            onReviewLike(reviewId, likedByMe)
                        }
                    },
                    onLoadReplies = onLoadReplies,
                    onReply = { reviewId, content ->
                        authorizeOrRun(BookDetailAuthenticatedAction.CreateReply(reviewId, content)) {
                            onReplyCreate(reviewId, content)
                        }
                    },
                    onReplyLike = { replyId, likedByMe ->
                        authorizeOrRun(BookDetailAuthenticatedAction.LikeReply(replyId, likedByMe)) {
                            onReplyLike(replyId, likedByMe)
                        }
                    },
                    onManageReview = { reviewActionTarget = it },
                    onManageReply = { replyActionTarget = it },
                )
            }
        }

        ComposeBar(
            onClick = {
                authorizeOrRun(BookDetailAuthenticatedAction.OpenReview) {
                    onReviewOpen { showReviewSheet = true }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 10.dp),
        )

        if (showScrollTop) {
            ScrollTopButton(
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 72.dp),
            )
        }
        if (state.isSubmitting) RequestLoadingOverlay(Modifier.fillMaxSize())
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showRatingDialog) {
        BookRatingDialog(
            currentBookId = book.id,
            initialRating = state.detail?.myRecord?.rating?.let { Rating.ofScore(it.toFloat()) },
            recentRatings = recentRatings,
            onDismiss = { showRatingDialog = false },
            onSave = { rating ->
                onRatingSave(rating) { showRatingDialog = false }
            },
        )
    }
    if (showPageDialog) {
        PageInputDialog(
            initialPage = currentPage,
            totalPages = book.totalPages,
            onDismiss = { showPageDialog = false },
            onSave = { page ->
                onPageSave(page)
                showPageDialog = false
            },
        )
    }
    if (showReviewSheet) {
        ReviewInputSheet(
            initialPage = currentPage,
            totalPages = book.totalPages,
            anonymous = state.signedIn && anonymousReviews,
            nickname = nickname,
            allowReadingProgress = state.signedIn,
            onDismiss = { showReviewSheet = false },
            onSave = { request ->
                onReviewCreate(request)
                showReviewSheet = false
            },
        )
    }
    reviewActionTarget?.let { review ->
        OwnedContentActionSheet(
            title = "내 감상 관리",
            onDismiss = { reviewActionTarget = null },
            onEdit = {
                reviewActionTarget = null
                editingReview = review
            },
            onDelete = {
                reviewActionTarget = null
                deletingReview = review
            },
        )
    }
    replyActionTarget?.let { reply ->
        OwnedContentActionSheet(
            title = "내 답글 관리",
            onDismiss = { replyActionTarget = null },
            onEdit = {
                replyActionTarget = null
                editingReply = reply
            },
            onDelete = {
                replyActionTarget = null
                deletingReply = reply
            },
        )
    }
    editingReview?.let { review ->
        ReviewInputSheet(
            initialPage = currentPage,
            totalPages = book.totalPages,
            anonymous = state.signedIn && anonymousReviews,
            nickname = nickname,
            initialReview = review,
            allowReadingProgress = state.signedIn,
            onDismiss = { editingReview = null },
            onSave = { request ->
                authorizeOrRun(BookDetailAuthenticatedAction.EditReview(review.reviewId, request)) {
                    onReviewUpdate(review.reviewId, request)
                }
                editingReview = null
            },
        )
    }
    editingReply?.let { reply ->
        ReplyInputSheet(
            initialContent = reply.content,
            onDismiss = { editingReply = null },
            onSave = { content ->
                authorizeOrRun(BookDetailAuthenticatedAction.EditReply(reply.replyId, content)) {
                    onReplyUpdate(reply.replyId, content)
                }
                editingReply = null
            },
        )
    }
    deletingReview?.let { review ->
        DeleteContentConfirmation(
            contentName = "감상",
            onDismiss = { deletingReview = null },
            onConfirm = {
                authorizeOrRun(BookDetailAuthenticatedAction.DeleteReview(review.reviewId)) {
                    onReviewDelete(review.reviewId)
                }
                deletingReview = null
            },
        )
    }
    deletingReply?.let { reply ->
        DeleteContentConfirmation(
            contentName = "답글",
            onDismiss = { deletingReply = null },
            onConfirm = {
                authorizeOrRun(BookDetailAuthenticatedAction.DeleteReply(reply.replyId)) {
                    onReplyDelete(reply.replyId)
                }
                deletingReply = null
            },
        )
    }
}

@Composable
private fun RequestLoadingOverlay(modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.background(ChaekInk.copy(alpha = 0.15f))
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
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ChaekAccent, strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun ArchiveStage(
    book: BookDetailArgs,
    saved: Boolean,
    onBack: () -> Unit,
    onLibraryClick: () -> Unit,
    coverContent: (@Composable (BookDetailArgs, Modifier) -> Unit)?,
) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(298.dp).background(ArchiveStageBackground)) {
        Surface(
            onClick = onBack,
            modifier = Modifier.offset(16.dp, 8.dp).size(38.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, ArchiveStageForeground),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back),
                    contentDescription = "뒤로 가기",
                    modifier = Modifier.size(20.dp),
                    tint = ArchiveStageForeground,
                )
            }
        }
        Surface(
            onClick = onLibraryClick,
            modifier = Modifier.align(Alignment.TopEnd).offset((-16).dp, 8.dp).size(38.dp),
            shape = RoundedCornerShape(4.dp),
            color = if (saved) ChaekAccent else Color.Transparent,
            border = if (saved) null else BorderStroke(1.dp, ArchiveStageForeground),
        ) {
            Box(
                modifier = Modifier.semantics {
                    contentDescription = if (saved) "서재에서 삭제" else "서재에 추가"
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_bookmark),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (saved) MaterialTheme.colorScheme.onPrimaryContainer else ArchiveStageForeground,
                )
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
            modifier = Modifier.align(Alignment.Center).offset(y = 5.dp).size(width = 164.dp, height = 217.dp)
                .background(ArchiveStageForeground).border(1.dp, ArchiveStageForeground)
                .shadow(10.dp, spotColor = ChaekAccent.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            val coverModifier = Modifier.size(width = 130.dp, height = 194.dp)
            if (coverContent == null) BookCover(book, coverModifier) else coverContent(book, coverModifier)
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
    Surface(modifier = modifier, color = ChaekSurfaceMuted, border = BorderStroke(1.dp, ChaekInk)) {
        Box(contentAlignment = Alignment.Center) {
            Text("책", style = MaterialTheme.typography.titleMedium)
            Image(
                painter = painterResource(coverResource(book.coverId)),
                contentDescription = "${book.title} 표지",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun coverResource(coverId: String): DrawableResource =
    when (coverId) {
        "cover-02" -> Res.drawable.cover_02
        "cover-03" -> Res.drawable.cover_03
        "cover-04" -> Res.drawable.cover_04
        "cover-05" -> Res.drawable.cover_05
        "cover-06" -> Res.drawable.cover_06
        "cover-07" -> Res.drawable.cover_07
        "cover-08" -> Res.drawable.cover_08
        "cover-09" -> Res.drawable.cover_09
        "cover-10" -> Res.drawable.cover_10
        "cover-11" -> Res.drawable.cover_11
        "cover-12" -> Res.drawable.cover_12
        "cover-13" -> Res.drawable.cover_13
        "cover-14" -> Res.drawable.cover_14
        "cover-15" -> Res.drawable.cover_15
        "cover-16" -> Res.drawable.cover_16
        "cover-17" -> Res.drawable.cover_17
        "cover-18" -> Res.drawable.cover_18
        "cover-19" -> Res.drawable.cover_19
        "cover-20" -> Res.drawable.cover_20
        else -> Res.drawable.cover_01
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
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
            ),
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
        }
        AverageRatingChip(averageRating)
    }
}

@Composable
private fun MetaChip(label: String) {
    Surface(modifier = Modifier.padding(horizontal = 4.dp).height(28.dp), shape = RoundedCornerShape(999.dp), color = ChaekBand) {
        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = ChaekInk, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AverageRatingChip(averageRating: Double?) {
    val ratingTenths = averageRating?.takeIf { it > 0.0 }?.let(::averageRatingInTenths)
    Surface(modifier = Modifier.padding(start = 4.dp).height(28.dp), shape = RoundedCornerShape(4.dp), color = ChaekAccentSoft) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Text("★★★★★", color = ChaekBorderSoft, fontSize = 15.sp)
                ratingTenths?.let { tenths ->
                    Text(
                        "★★★★★",
                        modifier = Modifier.drawWithContent {
                            clipRect(right = size.width * tenths / 50f) {
                                this@drawWithContent.drawContent()
                            }
                        },
                        color = ChaekAccent,
                        fontSize = 15.sp,
                    )
                }
            }
            Text(
                ratingTenths?.let { (it / 10.0).toString() } ?: "평가 없음",
                color = if (ratingTenths == null) ChaekInkSecondary else ChaekInk,
                fontSize = if (ratingTenths == null) 12.sp else 15.sp,
                fontWeight = if (ratingTenths == null) FontWeight.Normal else FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReadingRecord(
    book: BookDetailArgs,
    record: LibraryRecord?,
    currentPage: Int,
    onRate: () -> Unit,
    onStatusChange: (ReadingStatus) -> Unit,
    onPageInput: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Icon(painterResource(Res.drawable.ic_star), contentDescription = null, modifier = Modifier.size(12.dp), tint = ChaekInk)
                    Text(
                        record?.rating?.let { "별점 ${it} 수정" } ?: "별점 주기",
                        color = ChaekInk,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingStatus.entries.forEach { status ->
                val selected = record?.status == status.apiValue
                Surface(
                    onClick = { onStatusChange(status) },
                    modifier = Modifier.weight(1f).height(32.dp).semantics {
                        contentDescription = "독서 상태 ${status.label}"
                        this.selected = selected
                    },
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
                    Icon(painterResource(Res.drawable.ic_bookmark), contentDescription = null, modifier = Modifier.size(13.dp), tint = ChaekInkSecondary)
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
                    Icon(painterResource(Res.drawable.ic_pencil), contentDescription = null, modifier = Modifier.size(14.dp), tint = ChaekSurface)
                    Text("쪽수 입력", color = ChaekSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(999.dp)).background(ChaekBand)) {
            Box(
                Modifier.fillMaxWidth(
                    if (book.totalPages > 0) currentPage.toFloat().div(book.totalPages).coerceIn(0f, 1f) else 0f,
                ).height(5.dp).background(ChaekInk),
            )
        }
    }
}

@Composable
private fun ReviewsSection(
    reviews: List<BookReview>,
    reviewCount: Int,
    loading: Boolean,
    loadingMore: Boolean,
    revealedSpoilerReviewIds: Set<Long>,
    scope: ReviewScope,
    sort: ReviewSort,
    onScopeChange: (ReviewScope) -> Unit,
    onSortChange: (ReviewSort) -> Unit,
    onRevealSpoiler: (Long) -> Unit,
    onLike: (Long, Boolean) -> Unit,
    onLoadReplies: (Long) -> Unit,
    onReply: (Long, String) -> Unit,
    onReplyLike: (Long, Boolean) -> Unit,
    onManageReview: (BookReview) -> Unit,
    onManageReply: (ReviewReply) -> Unit,
) {
    var replyTarget by remember { mutableStateOf<BookReview?>(null) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    Icon(painterResource(Res.drawable.ic_chevron_down), contentDescription = null, modifier = Modifier.size(11.dp), tint = ChaekInk)
                }
            }
            Surface(shape = RoundedCornerShape(999.dp), color = Color.Transparent, border = BorderStroke(1.dp, ChaekBorder)) {
                Row(modifier = Modifier.padding(2.dp)) {
                    FeedScopeChip("전체 피드", scope == ReviewScope.ALL) { onScopeChange(ReviewScope.ALL) }
                    FeedScopeChip("내 피드", scope == ReviewScope.MINE) { onScopeChange(ReviewScope.MINE) }
                }
            }
        }
        when {
            loading -> Text("감상을 불러오는 중이에요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            reviews.isEmpty() -> Text("아직 등록된 감상이 없어요", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            else -> reviews.forEach { review ->
                val locked = shouldLockReview(review.reviewId, review.isSpoiler, revealedSpoilerReviewIds)
                ReviewCard(
                    review = review,
                    locked = locked,
                    onOpenLockedReview = { onRevealSpoiler(review.reviewId) },
                    onLike = onLike,
                    onLoadReplies = { onLoadReplies(review.reviewId) },
                    onReply = {
                        if (locked) onRevealSpoiler(review.reviewId) else replyTarget = review
                    },
                    onReplyLike = onReplyLike,
                    onManage = { onManageReview(review) },
                    onManageReply = onManageReply,
                )
            }
        }
        if (loadingMore) {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).semantics { contentDescription = "감상 더 불러오는 중" },
                    color = ChaekAccent,
                    strokeWidth = 2.dp,
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

@Composable
private fun FeedScopeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(24.dp).clip(RoundedCornerShape(999.dp))
            .background(if (selected) ChaekInk else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) ChaekSurface else ChaekInkSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
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
    onManage: () -> Unit,
    onManageReply: (ReviewReply) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().then(
            if (locked) {
                Modifier.clickable(role = Role.Button, onClick = onOpenLockedReview)
                    .clearAndSetSemantics {
                        contentDescription = "스포일러 감상 가림. 탭해서 보기"
                        role = Role.Button
                        onClick {
                            onOpenLockedReview()
                            true
                        }
                    }
            } else {
                Modifier
            },
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(ChaekBackground).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AuthorAvatar(review.authorName, review.authorProfileImageUrl, 34)
                Column(modifier = Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(review.authorName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        reviewMetadata(review.createdAt, review.currentPage),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (canManageContent(review.writtenByMe, review.deleted)) {
                    Box(
                        modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onManage)
                            .semantics { contentDescription = "내 감상 수정 또는 삭제" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⋯", color = ChaekInkSecondary, fontSize = 20.sp)
                    }
                }
            }
            Text(
                if (locked) maskAsChirps(review.content) else review.content,
                fontSize = 12.5.sp,
                lineHeight = 21.sp,
            )
            review.quote?.let { quote ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(ChaekAccentSoft)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReviewAction(
                    icon = if (review.likedByMe) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline,
                    label = "좋아요 ${review.likeCount}",
                    description = if (review.likedByMe) "감상 좋아요 취소" else "감상 좋아요",
                ) { onLike(review.reviewId, review.likedByMe) }
                Row(modifier = Modifier.clickable(role = Role.Button, onClick = onReply), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(Res.drawable.ic_comment), contentDescription = "감상에 답글 작성", modifier = Modifier.size(14.dp))
                    Text("답글 ${review.replyCount}", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
            }
        }
        if (review.replyCount > 0) {
            Replies(
                review.recentReplies,
                review.replyCount,
                locked,
                onOpenLockedReview,
                onLoadReplies,
                onReplyLike,
                onManageReply,
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
    onManage: (ReviewReply) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(ChaekSurfaceMuted)
            .padding(start = 48.dp, top = 12.dp, end = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        replies.forEach { reply ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                AuthorAvatar(reply.authorName, reply.authorProfileImageUrl, 24)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        reply.authorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (locked) maskAsChirps(reply.content) else reply.content,
                        color = ChaekInkSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                    )
                }
                if (canManageContent(reply.writtenByMe, reply.deleted)) {
                    Box(
                        modifier = Modifier.size(40.dp).clickable(role = Role.Button) { onManage(reply) }
                            .semantics { contentDescription = "내 답글 수정 또는 삭제" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⋯", color = ChaekInkSecondary, fontSize = 18.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(if (reply.likedByMe) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline),
                        contentDescription = if (reply.likedByMe) "답글 좋아요 취소" else "답글 좋아요",
                        modifier = Modifier.size(13.dp).clickable(role = Role.Button) {
                            onLike(reply.replyId, reply.likedByMe)
                        },
                        tint = ChaekInkSecondary,
                    )
                    Text("${reply.likeCount}", color = ChaekInkSecondary, fontFamily = FontFamily.Monospace, fontSize = 9.5.sp)
                }
            }
        }
        if (replies.size < totalCount) {
            Text(
                "답글 ${totalCount}개 모두 보기",
                modifier = Modifier.clickable(role = Role.Button, onClick = if (locked) onOpenLockedReview else onLoadAll)
                    .padding(vertical = 4.dp),
                color = ChaekInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AuthorAvatar(name: String, profileImageUrl: String?, size: Int) {
    val modifier = Modifier.size(size.dp).clip(CircleShape)
    if (profileImageUrl.isNullOrBlank()) {
        Image(painterResource(authorAvatarResource(name)), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        LocalRemoteBookCover.current(profileImageUrl, "", modifier)
    }
}

private fun authorAvatarResource(displayName: String): DrawableResource =
    if ((displayName.hashCode() and Int.MAX_VALUE) % 2 == 0) Res.drawable.avatar_kim else Res.drawable.avatar_yoon

@Composable
private fun ReviewAction(icon: DrawableResource, label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = description }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(14.dp), tint = ChaekInk)
        Text(label, color = ChaekInk, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }
}

@Composable
private fun ComposeBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = ChaekSurface,
        border = BorderStroke(1.dp, ChaekBorder),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(6.dp), color = ChaekSurfaceMuted) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("✎", color = ChaekInkSecondary, fontSize = 14.sp)
                    Text("이 순간의 감상 남기기", modifier = Modifier.padding(start = 8.dp), color = ChaekInkSecondary, fontSize = 11.sp)
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
