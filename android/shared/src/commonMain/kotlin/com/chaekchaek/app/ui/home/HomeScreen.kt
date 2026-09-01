package com.chaekchaek.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
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
import com.chaekchaek.app.presentation.common.AppError
import com.chaekchaek.app.ui.common.ChaekAlertDialog
import com.chaekchaek.app.presentation.home.FeedSectionUiModel
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.home.OverlappedCardUiModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.*
import com.chaekchaek.app.ui.theme.ChaekBand
import kotlin.math.abs
import kotlin.random.Random
import kotlinx.coroutines.delay

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
    accessToken: String? = null,
    scrollTopRequest: Int = 0,
    modifier: Modifier = Modifier,
    onSearchBook: () -> Unit = {},
    onBookClick: (BookDetailTarget) -> Unit = {},
) {
    val uiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(accessToken) {
        homeViewModel.authenticate(accessToken)
    }

    when (val state = uiState) {
        HomeUiState.Loading -> LoadingContent(modifier)
        HomeUiState.Empty -> EmptyContent(modifier)
        is HomeUiState.Failure -> ErrorContent(state.error, homeViewModel::retry, modifier)
        is HomeUiState.Content -> HomeContent(state, onSearchBook, onBookClick, scrollTopRequest, modifier)
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
private fun ErrorContent(error: AppError, retry: () -> Unit, modifier: Modifier) {
    Box(modifier.fillMaxSize())
    ChaekAlertDialog(
        onDismissRequest = {},
        title = { Text("홈을 불러오지 못했어요") },
        text = { Text(error.message()) },
        confirmButton = { TextButton(onClick = retry) { Text("다시 시도") } },
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState.Content,
    onSearchBook: () -> Unit,
    onBookClick: (BookDetailTarget) -> Unit,
    scrollTopRequest: Int,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollTopRequest) {
        if (scrollTopRequest > 0) listState.animateScrollToItem(0)
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        state = listState,
    ) {
        item { HomeHeader() }
        items(state.sections) { section ->
            when (section) {
                is FeedSectionUiModel.TrendingBooks -> TrendingSection(section, onBookClick)
                is FeedSectionUiModel.RecentQuotes -> RecentReflectionsSection(
                    title = section.title,
                    quotes = section.cards,
                    onBookClick = onBookClick,
                )
                is FeedSectionUiModel.OverlappedBooks -> RecentReflectionsSection(
                    title = section.title,
                    overlapped = section.cards,
                    onBookClick = onBookClick,
                )
            }
        }
        item { ReadingStatusSection(state.readingBook, onSearchBook, onBookClick) }
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
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.avatar_yoon),
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
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val selectedBook = books.getOrNull(selectedIndex)
        ?: books.firstOrNull()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .background(ChaekBand)
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
                                selectedIndex = next
                                handled = true
                            }
                        }
                    },
                )
            },
    ) {
        val heroWidth = maxWidth
        books.forEachIndexed { index, book ->
            val placement = placements[collageSlotIndex(index, selectedIndex, books.size)]
            val x by animateFloatAsState(
                heroWidth.value * (placement.x / 390f),
                label = "${book.bookId.value} x",
            )
            val y by animateFloatAsState(placement.y.toFloat(), label = "${book.bookId.value} y")
            val scaleX by animateFloatAsState(
                placement.width / HERO_COVER_WIDTH,
                label = "${book.bookId.value} 너비",
            )
            val scaleY by animateFloatAsState(
                placement.height / HERO_COVER_HEIGHT,
                label = "${book.bookId.value} 높이",
            )
            val rotation by animateFloatAsState(
                placement.rotation,
                label = "${book.bookId.value} 회전",
            )
            HeroCover(
                book = book,
                rank = index + 1,
                selected = selectedIndex == index,
                onClick = { onBookClick(book.toBookDetailTarget()) },
                x = x,
                y = y,
                scaleX = scaleX,
                scaleY = scaleY,
                rotation = rotation,
            )
        }

        selectedBook?.let { book ->
            Surface(
                onClick = { onBookClick(book.toBookDetailTarget()) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 190.dp)
                    .size(width = 150.dp, height = 38.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 3.dp,
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
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
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
    x: Float,
    y: Float,
    scaleX: Float,
    scaleY: Float,
    rotation: Float,
) {
    if (book == null) return
    Cover(
        coverId = book.coverId,
        title = "${rank}위, ${book.title}",
        modifier = Modifier
            .zIndex(if (selected) 1f else 0f)
            .size(HERO_COVER_WIDTH.dp, HERO_COVER_HEIGHT.dp)
            .graphicsLayer {
                translationX = x.dp.toPx()
                translationY = y.dp.toPx()
                this.scaleX = scaleX
                this.scaleY = scaleY
                rotationZ = rotation
                transformOrigin = TransformOrigin(0f, 0f)
                shadowElevation = if (selected) 12.dp.toPx() else 0f
                shape = RoundedCornerShape(2.dp)
                clip = false
            }
            .semantics {
                this.selected = selected
            }
            .clickable(role = Role.Button, onClick = onClick),
    )
}

private const val HERO_COVER_WIDTH = 118f
private const val HERO_COVER_HEIGHT = 177f

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
    val random = Random(bookIds.take(6).fold(17) { seed, id -> seed * 31 + id.hashCode() })
    val lowerSlot = if (random.nextBoolean()) Triple(121, 92, -7f) else Triple(213, 92, 7f)
    val slots = listOf(
        Triple(120, 4, listOf(-2f, 0f, 2f).random(random)),
        Triple(251, 31, 9f),
        Triple(64, 29, -5f),
        Triple(286, 68, 11f),
        Triple(31, 63, -10f),
        lowerSlot,
    )
    val sizes = listOf(118 to 177, 80 to 120, 70 to 105, 66 to 99, 63 to 94, 56 to 80)

    return slots.zip(sizes).mapIndexed { index, (slot, size) ->
        CollagePlacement(
            x = slot.first + random.nextInt(-1, 2) * 8,
            y = slot.second + if (index == 0) 0 else random.nextInt(-1, 2) * 8,
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
    onBookClick: (BookDetailTarget) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(quotes, key = { it.noteId.value }) { card ->
                ReflectionCard(
                    title = card.bookTitle,
                    coverId = card.coverId,
                    authorLabel = card.authorLabel,
                    excerpt = card.quoteText,
                    replyLabel = card.replyLabel,
                    avatar = Res.drawable.avatar_kim,
                    profileImageUrl = card.authorProfileImageUrl,
                    onClick = { onBookClick(card.toBookDetailTarget()) },
                )
            }
            items(overlapped, key = { it.bookId.value }) { card ->
                ReflectionCard(
                    title = card.title,
                    coverId = card.coverId,
                    authorLabel = card.authorLabel,
                    excerpt = card.excerpt,
                    replyLabel = card.replyLabel,
                    avatar = Res.drawable.avatar_yoon,
                    onClick = { onBookClick(card.toBookDetailTarget()) },
                )
            }
        }
    }
}

@Composable
private fun ReflectionCard(
    title: String,
    coverId: String,
    authorLabel: String,
    excerpt: String,
    replyLabel: String,
    avatar: DrawableResource,
    profileImageUrl: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 318.dp, height = 184.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReflectionCover(
                title = title,
                coverId = coverId,
                modifier = Modifier
                    .width(107.dp)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "›",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                    )
                }
                AuthorLine(authorLabel, avatar, profileImageUrl, imageSize = 20.dp)
                Text(
                    "“$excerpt”",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_comment),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        replyLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    label: String,
    avatar: DrawableResource,
    profileImageUrl: String?,
    imageSize: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val imageModifier = Modifier.size(imageSize).clip(CircleShape)
        if (profileImageUrl.isNullOrBlank()) {
            Image(painterResource(avatar), null, imageModifier, contentScale = ContentScale.Crop)
        } else {
            LocalRemoteBookCover.current(profileImageUrl, "", imageModifier)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

private fun AppError.message(): String =
    when (this) {
        AppError.Network -> "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        AppError.NotFound -> "홈 피드를 찾을 수 없어요."
        AppError.Unauthorized -> "로그인이 필요한 요청이에요."
        AppError.Unknown -> "잠시 후 다시 시도해 주세요."
    }

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
