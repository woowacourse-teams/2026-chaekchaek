package com.chaekchaek.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.runtime.collectAsState
import com.chaekchaek.app.presentation.home.FeedSectionUiModel
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.home.OverlappedCardUiModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import com.chaekchaek.app.ui.common.avatarResource
import com.chaekchaek.app.ui.common.BrandHeader
import com.chaekchaek.app.ui.common.HomeLoadErrorContent
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.*
import com.chaekchaek.app.ui.theme.ChaekBand
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.collageBookShadow
import com.chaekchaek.app.ui.theme.recentBookShadow
import com.chaekchaek.app.ui.theme.continueReadingShadow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BookDetailTarget(
    val id: String,
    val isbn13: String = "",
    val bookId: Long? = null,
    val title: String,
    val creator: String = "",
    val publisher: String = "",
    val year: String = "",
    val category: String = "",
    val totalPages: Int = 0,
    val coverUrl: String = "",
    val coverId: String = "",
)

typealias RemoteBookCover = @Composable (url: String, contentDescription: String, modifier: Modifier) -> Unit

val LocalRemoteBookCover = staticCompositionLocalOf<RemoteBookCover> {
    { _, description, modifier ->
        Image(
            painter = painterResource(Res.drawable.app_logo_square),
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    myDisplayName: String,
    accessToken: String? = null,
    scrollTopRequest: Int = 0,
    modifier: Modifier = Modifier,
    onSearchBook: () -> Unit = {},
    onOpenFeed: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onBookClick: (BookDetailTarget) -> Unit = {},
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(accessToken) {
        homeViewModel.authenticate(accessToken)
    }

    when (val state = uiState) {
        HomeUiState.Loading -> LoadingContent(modifier)
        HomeUiState.Empty -> EmptyContent(modifier)
        is HomeUiState.Failure -> HomeLoadErrorContent(state.error, homeViewModel::retry, modifier)
        is HomeUiState.Content -> HomeContent(
            state,
            myDisplayName,
            onSearchBook,
            onOpenFeed,
            onProfileClick,
            onBookClick,
            scrollTopRequest,
            modifier,
        )
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(500)
        visible = true
    }
    if (visible) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("아직 도착한 책 이야기가 없어요.", style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    myDisplayName: String,
    onSearchBook: () -> Unit,
    onOpenFeed: () -> Unit,
    onProfileClick: () -> Unit,
    onBookClick: (BookDetailTarget) -> Unit,
    scrollTopRequest: Int,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollTopRequest) {
        if (scrollTopRequest > 0) listState.animateScrollToItem(0)
    }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 76.dp),
        ) {
            item { BrandHeader(onProfileClick) }
            items(state.sections) { section ->
                when (section) {
                    is FeedSectionUiModel.TrendingBooks -> TrendingSection(section, onBookClick)
                    is FeedSectionUiModel.RecentQuotes -> RecentReflectionsSection(
                        title = section.title,
                        quotes = section.cards,
                        onOpenFeed = onOpenFeed,
                        onBookClick = onBookClick,
                    )
                    is FeedSectionUiModel.OverlappedBooks -> Unit
                }
            }
        }
        val readingBook = state.readingBook
        StickyReadingBar(
            book = readingBook,
            onClick = {
                if (readingBook == null) onSearchBook()
                else onBookClick(readingBook.toBookDetailTarget())
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StickyReadingBar(book: ReadingBookUiModel?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .continueReadingShadow()
            .height(89.dp),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF191919),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (book == null) {
                Box(
                    modifier = Modifier
                        .size(width = 42.dp, height = 60.dp)
                        .background(Color(0xFFF1F1F3), RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "?",
                        color = Color(0xFF555555),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        fontFamily = ChaekIconFontFamily(),
                    )
                }
            } else {
                Cover(book.coverId, book.title, Modifier.size(width = 42.dp, height = 60.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (book == null) "읽고 있는 책" else "이어서 읽기",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    book?.title ?: "지금 읽고 있는 책이 있으세요?",
                    color = Color.White,
                    style = if (book == null) {
                        MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book == null) {
                    Text(
                        "책 제목으로 찾기",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "${book.currentPage} / ${book.totalPages}쪽",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "새 감상 -",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.2f))) {
                    if (book != null) {
                        Box(
                            Modifier.fillMaxWidth(readingProgress(book.currentPage, book.totalPages)).height(3.dp)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                    }
                }
            }
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "↗",
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = ChaekIconFontFamily(),
                )
            }
        }
    }
}

@Composable
private fun ReadingStatusSection(
    readingBook: ReadingBookUiModel?,
    onSearchBook: () -> Unit,
    onBookClick: (BookDetailTarget) -> Unit,
) {
    if (readingBook == null) {
        EmptyReadingSection(onSearchBook)
    } else {
        CurrentReadingSection(readingBook, onBookClick)
    }
}

@Composable
private fun CurrentReadingSection(book: ReadingBookUiModel, onBookClick: (BookDetailTarget) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground, thickness = 1.dp)
        Spacer(Modifier.height(22.dp))
        Text(
            "이어서 읽기",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = { onBookClick(book.toBookDetailTarget()) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Cover(
                    coverId = book.coverId,
                    title = book.title,
                    modifier = Modifier.size(width = 52.dp, height = 78.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(78.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            book.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "›",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 17.sp,
                            lineHeight = 18.sp,
                        )
                    }
                    Text(
                        "${book.currentPage} / ${book.totalPages}쪽",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(ChaekBand),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(readingProgress(book.currentPage, book.totalPages))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.onSurface),
                        )
                    }
                    Text(
                        "이어서 기록하기  ↗",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReadingSection(onSearchBook: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 17.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "지금 읽고 있는 책이 있으세요?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                "책을 등록하면 읽은 쪽수와 감상을 남길 수 있어요.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onSearchBook)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "책 제목으로 찾기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun readingProgress(currentPage: Int, totalPages: Int): Float =
    if (totalPages <= 0) 0f else currentPage.toFloat().div(totalPages).coerceIn(0f, 1f)

@Composable
private fun HomeHeader(displayName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.mascot_outline_b),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            "책췍",
            modifier = Modifier.padding(start = 7.dp),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
        )
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(avatarResource(displayName)),
            contentDescription = "내 프로필",
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun TrendingSection(
    section: FeedSectionUiModel.TrendingBooks,
    onBookClick: (BookDetailTarget) -> Unit,
) {
    val books = section.books.take(6)
    val rankingKey = books.map { it.bookId.value }
    val placements = remember(rankingKey) { collagePlacements(rankingKey) }
    var selectedIndex by rememberSaveable(rankingKey) { mutableIntStateOf(0) }
    var previousSelectedIndex by rememberSaveable(rankingKey) { mutableIntStateOf(selectedIndex) }
    val transitionProgress = remember(rankingKey) { Animatable(0f) }
    val transitionScope = rememberCoroutineScope()
    var transitionInProgress by remember(rankingKey) { mutableStateOf(false) }
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val selectedBook = books.getOrNull(selectedIndex)
        ?: books.firstOrNull()
    val requestSelection by rememberUpdatedState<(Int) -> Unit> { nextIndex ->
        val currentIndex = selectedIndex
        if (nextIndex != currentIndex && nextIndex in books.indices && !transitionInProgress) {
            transitionInProgress = true
            transitionScope.launch {
                previousSelectedIndex = currentIndex
                selectedIndex = nextIndex
                transitionProgress.snapTo(0f)
                transitionProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = COLLAGE_TRANSITION_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                )
                previousSelectedIndex = nextIndex
                transitionProgress.snapTo(0f)
                transitionInProgress = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(297.dp)
            .padding(top = 13.dp)
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .selectableGroup()
            .pointerInput(rankingKey) {
                var dragDistance = 0f
                var handled = false
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                        handled = false
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        if (!handled) {
                            dragDistance += amount
                            val next = collageSelectionAfterSwipe(
                                current = currentSelectedIndex,
                                bookCount = books.size,
                                dragDistance = dragDistance,
                                threshold = 48.dp.toPx(),
                            )
                            if (next != currentSelectedIndex) {
                                requestSelection(next)
                                handled = true
                            }
                        }
                    },
                )
            },
    ) {
        val canvasScale = (maxWidth / 390.dp).coerceAtMost(1f)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = 27.dp).fillMaxWidth().padding(horizontal = 27.dp)
                .height(191.dp).background(
                    ChaekBand,
                    RoundedCornerShape(topStart = 160.dp, topEnd = 160.dp, bottomStart = 26.dp, bottomEnd = 26.dp),
                ),
        )
        books.forEachIndexed { index, book ->
            val previousSlot = collageSlotIndex(index, previousSelectedIndex, books.size)
            val slot = collageSlotIndex(index, selectedIndex, books.size)
            HeroCover(
                book = book,
                rank = index + 1,
                selected = selectedIndex == index,
                onClick = { onBookClick(book.toBookDetailTarget()) },
                startTransform = collageTransform(placements[previousSlot], canvasScale),
                endTransform = collageTransform(placements[slot], canvasScale),
                transitionProgress = { transitionProgress.value },
                canvasScale = canvasScale,
                layerOrder = 6f - slot,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 184.dp).height(32.dp).zIndex(10f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            books.forEachIndexed { index, _ ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier.width(if (selected) 28.dp else 15.dp).height(32.dp)
                        .semantics {
                            contentDescription = "${index + 1}번째 인기 책 보기"
                            this.selected = selected
                        }
                        .clickable(role = Role.Tab) { requestSelection(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = if (selected) 20.dp else 5.dp, height = 5.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.onSurface else Color(0xFFD8D8D8),
                                RoundedCornerShape(10.dp),
                            ),
                    )
                }
            }
        }

        selectedBook?.let { book ->
            Surface(
                onClick = { onBookClick(book.toBookDetailTarget()) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 218.dp)
                    .height(44.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(0.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    Text("  ↗", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    book.noteCountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    book.replyCountLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeroCover(
    book: TrendingBookUiModel?,
    rank: Int,
    selected: Boolean,
    onClick: () -> Unit,
    startTransform: CollageTransform,
    endTransform: CollageTransform,
    transitionProgress: () -> Float,
    canvasScale: Float,
    layerOrder: Float,
) {
    if (book == null) return
    Cover(
        coverId = book.coverId,
        title = "${rank}위, ${book.title}",
        modifier = Modifier
            .zIndex(layerOrder)
            .size(COLLAGE_BASE_WIDTH.dp * canvasScale, COLLAGE_BASE_HEIGHT.dp * canvasScale)
            .graphicsLayer {
                val progress = transitionProgress()
                translationX = lerp(startTransform.translationX, endTransform.translationX, progress).dp.toPx()
                translationY = lerp(startTransform.translationY, endTransform.translationY, progress).dp.toPx()
                scaleX = lerp(startTransform.scaleX, endTransform.scaleX, progress)
                scaleY = lerp(startTransform.scaleY, endTransform.scaleY, progress)
                rotationZ = lerp(startTransform.rotation, endTransform.rotation, progress)
                transformOrigin = TransformOrigin.Center
                shape = RoundedCornerShape(2.dp)
                clip = false
            }
            .collageBookShadow()
            .semantics {
                this.selected = selected
            }
            .clickable(role = Role.Button, onClick = onClick),
    )
}

private const val COLLAGE_BASE_WIDTH = 108f
private const val COLLAGE_BASE_HEIGHT = 155f
private const val COLLAGE_TRANSITION_MILLIS = 260

internal data class CollageTransform(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
)

internal fun collageTransform(placement: CollagePlacement, canvasScale: Float): CollageTransform {
    val radians = placement.rotation * PI / 180
    val rotatedWidth = abs(placement.width * cos(radians)) + abs(placement.height * sin(radians))
    val rotatedHeight = abs(placement.width * sin(radians)) + abs(placement.height * cos(radians))
    return CollageTransform(
        translationX = (placement.x + rotatedWidth / 2 - COLLAGE_BASE_WIDTH / 2).toFloat() * canvasScale,
        translationY = (placement.y + rotatedHeight / 2 - COLLAGE_BASE_HEIGHT / 2).toFloat() * canvasScale,
        scaleX = placement.width / COLLAGE_BASE_WIDTH,
        scaleY = placement.height / COLLAGE_BASE_HEIGHT,
        rotation = placement.rotation,
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

internal data class CollagePlacement(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val rotation: Float,
)

internal fun collageSlotIndex(bookIndex: Int, selectedIndex: Int, bookCount: Int): Int {
    if (bookCount <= 1) return 0
    val relative = ((bookIndex - selectedIndex) % bookCount + bookCount) % bookCount
    return listOf(0, 1, 3, 5, 4, 2).filter { it < bookCount }[relative]
}

internal fun collageSelectionAfterSwipe(
    current: Int,
    bookCount: Int,
    dragDistance: Float,
    threshold: Float,
): Int {
    if (bookCount <= 1 || abs(dragDistance) < threshold) return current
    return if (dragDistance < 0) (current + 1) % bookCount else (current - 1 + bookCount) % bookCount
}

internal fun collagePlacements(bookIds: List<String>): List<CollagePlacement> {
    val slots = listOf(
        Triple(141, 29, 0f),
        Triple(225, 53, 13f),
        Triple(63, 48, -15f),
        Triple(271, 84, 24f),
        Triple(23, 80, -23f),
        Triple(172, 4, 10f),
    )
    val sizes = listOf(108 to 155, 79 to 113, 79 to 113, 65 to 93, 65 to 93, 80 to 114)

    return slots.zip(sizes).mapIndexed { index, (slot, size) ->
        CollagePlacement(
            x = slot.first,
            y = slot.second,
            width = size.first,
            height = size.second,
            rotation = slot.third,
        )
    }
}

@Composable
private fun RecentReflectionsSection(
    title: String,
    quotes: List<QuoteCardUiModel> = emptyList(),
    overlapped: List<OverlappedCardUiModel> = emptyList(),
    onOpenFeed: (() -> Unit)? = null,
    onBookClick: (BookDetailTarget) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 27.dp, end = 24.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            onOpenFeed?.let { openFeed ->
                TextButton(onClick = openFeed) {
                    Text("감상 더보기", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = recentReflectionCardWidth(maxWidth)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(211.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(quotes, key = { it.noteId.value }) { card ->
                    ReflectionCard(
                        title = card.bookTitle,
                        coverId = card.coverId,
                        authorName = card.authorName,
                        timeLabel = card.timeLabel,
                        excerpt = card.quoteText,
                        replyLabel = card.replyLabel,
                        width = cardWidth,
                        onClick = { onBookClick(card.toBookDetailTarget()) },
                    )
                }
                items(overlapped, key = { it.bookId.value }) { card ->
                    ReflectionCard(
                        title = card.title,
                        coverId = card.coverId,
                        authorName = card.authorName,
                        timeLabel = card.timeLabel,
                        excerpt = card.excerpt,
                        replyLabel = card.replyLabel,
                        width = cardWidth,
                        onClick = { onBookClick(card.toBookDetailTarget()) },
                    )
                }
            }
        }
    }
}

internal fun recentReflectionCardWidth(availableWidth: Dp): Dp = minOf(314.dp, availableWidth)

@Composable
private fun ReflectionCard(
    title: String,
    coverId: String,
    authorName: String,
    timeLabel: String,
    excerpt: String,
    replyLabel: String,
    width: Dp,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = width, height = 205.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFF3F3F5),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(123.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ReflectionCover(
                    title,
                    coverId,
                    Modifier.size(width = 64.dp, height = 92.dp).recentBookShadow(),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    AuthorLine(
                        authorName = authorName,
                        timeLabel = timeLabel,
                        imageSize = 18.dp,
                    )
                    Text(
                        excerpt,
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.outline,
                )
                Row(
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_comment),
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        replyLabel,
                        modifier = Modifier.padding(start = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "↗",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = ChaekIconFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReflectionCover(title: String, coverId: String, modifier: Modifier = Modifier) {
    if (title == "보이지 않는 도시") {
        InvisibleCitiesCover(modifier)
    } else {
        Cover(coverId, title, modifier)
    }
}

private val InvisibleCitiesCoverPaper = Color(0xFFF0F0EC)
private val InvisibleCitiesCoverInk = Color(0xFF171717)
private val InvisibleCitiesCoverBlock = Color(0xFF252525)

@Composable
private fun InvisibleCitiesCover(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(InvisibleCitiesCoverPaper)
            .border(.5.dp, Color.Black.copy(alpha = .12f), RoundedCornerShape(2.dp)),
    ) {
        Text(
            "LE CITTÀ\nINVISIBILI",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 7.dp, top = 7.dp),
            color = InvisibleCitiesCoverInk,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Box(
            Modifier
                .offset(x = 27.dp, y = 51.dp)
                .size(width = 67.dp, height = 73.dp)
                .graphicsLayer { rotationZ = 8f }
                .background(InvisibleCitiesCoverBlock),
        )
        Text(
            "看不見的城市",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp),
            color = InvisibleCitiesCoverBlock,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AuthorLine(
    authorName: String,
    timeLabel: String,
    imageSize: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val imageModifier = Modifier.size(imageSize).clip(CircleShape)
        Image(painterResource(avatarResource(authorName)), null, imageModifier, contentScale = ContentScale.Crop)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                authorName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Cover(coverId: String, title: String, modifier: Modifier = Modifier) {
    val coverModifier = modifier
        .border(.5.dp, Color.Black.copy(alpha = .2f), RoundedCornerShape(2.dp))
        .clip(RoundedCornerShape(2.dp))
    if (coverId.isRemoteCoverUrl()) {
        LocalRemoteBookCover.current(coverId, "$title 표지", coverModifier)
    } else {
        Image(
            painter = painterResource(coverResource(coverId)),
            contentDescription = "$title 표지",
            modifier = coverModifier,
            contentScale = ContentScale.Crop,
        )
    }
}

internal fun String.isRemoteCoverUrl(): Boolean = startsWith("https://")

internal fun coverResource(coverId: String): DrawableResource =
    when (coverId) {
        "cover-01" -> Res.drawable.cover_01
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
        else -> Res.drawable.app_logo_square
    }

private fun TrendingBookUiModel.toBookDetailTarget() = BookDetailTarget(
    id = bookId.value,
    isbn13 = isbn13,
    bookId = bookId.value.toLongOrNull(),
    title = title,
    coverId = coverId,
)

private fun QuoteCardUiModel.toBookDetailTarget() = BookDetailTarget(
    id = bookId.value,
    isbn13 = isbn13,
    bookId = bookId.value.toLongOrNull(),
    title = bookTitle,
    coverId = coverId,
)

private fun OverlappedCardUiModel.toBookDetailTarget() =
    BookDetailTarget(id = bookId.value, title = title, coverId = coverId)

private fun ReadingBookUiModel.toBookDetailTarget() = BookDetailTarget(
    id = bookId.value,
    isbn13 = isbn13,
    bookId = bookId.value.toLongOrNull(),
    title = title,
    totalPages = totalPages,
    coverId = coverId,
)
