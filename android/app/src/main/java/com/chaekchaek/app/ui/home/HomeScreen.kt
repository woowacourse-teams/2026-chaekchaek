package com.chaekchaek.app.ui.home

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaekchaek.app.LocalSharedComponent
import com.chaekchaek.app.R
import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.domain.note.NoteId
import com.chaekchaek.app.presentation.common.AppError
import com.chaekchaek.app.presentation.home.FeedSectionUiModel
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.home.OverlappedCardUiModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.ReadingBookUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import com.chaekchaek.app.theme.ChaekAccent
import com.chaekchaek.app.theme.ChaekBand
import com.chaekchaek.app.theme.ChaekBorder
import com.chaekchaek.app.theme.ChaekBorderSoft
import com.chaekchaek.app.theme.ChaekInk
import com.chaekchaek.app.theme.ChaekInkSecondary
import com.chaekchaek.app.theme.ChaekInkTertiary
import com.chaekchaek.app.theme.ChaekSurface
import com.chaekchaek.app.theme.ChaekchaekTheme
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSearchBook: () -> Unit = {},
) {
    val component = LocalSharedComponent.current
    val homeViewModel: HomeViewModel = viewModel { component.homeViewModel }
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        HomeUiState.Loading -> LoadingContent(modifier)
        HomeUiState.Empty -> EmptyContent(modifier)
        is HomeUiState.Failure -> ErrorContent(state.error, homeViewModel::retry, modifier)
        is HomeUiState.Content -> HomeContent(state, onSearchBook, modifier)
    }
}

@Composable
private fun LoadingContent(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ChaekAccent)
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
    AlertDialog(
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
    modifier: Modifier,
) {
    val trending = state.sections.filterIsInstance<FeedSectionUiModel.TrendingBooks>().firstOrNull()
    val recent = state.sections.filterIsInstance<FeedSectionUiModel.RecentQuotes>().firstOrNull()
    val overlapped = state.sections.filterIsInstance<FeedSectionUiModel.OverlappedBooks>().firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        item { HomeHeader() }
        trending?.let { item { TrendingSection(it) } }
        if (recent != null || overlapped != null) {
            item {
                RecentReflectionsSection(
                    quotes = recent?.cards.orEmpty(),
                    overlapped = overlapped?.cards.orEmpty(),
                )
            }
        }
        item { ReadingStatusSection(state.readingBook, onSearchBook) }
    }
}

@Composable
private fun ReadingStatusSection(
    readingBook: ReadingBookUiModel?,
    onSearchBook: () -> Unit,
) {
    if (readingBook == null) {
        EmptyReadingSection(onSearchBook)
    } else {
        CurrentReadingSection(readingBook)
    }
}

@Composable
private fun CurrentReadingSection(book: ReadingBookUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
    ) {
        HorizontalDivider(color = ChaekInk, thickness = 1.dp)
        Spacer(Modifier.height(22.dp))
        Text(
            "이어서 읽기",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal),
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ChaekSurface,
            border = BorderStroke(1.dp, ChaekBorderSoft),
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
                        Text("›", color = ChaekInkSecondary, fontSize = 18.sp, lineHeight = 18.sp)
                    }
                    Text(
                        "${book.currentPage} / ${book.totalPages}쪽",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChaekInkSecondary,
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
                                .background(ChaekInk),
                        )
                    }
                    Text(
                        "이어서 기록하기  ↗",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = ChaekInkSecondary,
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
        color = ChaekSurface,
        border = BorderStroke(1.dp, ChaekBorderSoft),
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
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                color = ChaekInkSecondary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, ChaekBorder, RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onSearchBook)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ChaekInkSecondary,
                )
                Text(
                    "책 제목으로 찾기",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChaekInkTertiary,
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Image(
            painter = painterResource(R.drawable.avatar_yoon),
            contentDescription = "내 프로필",
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp)),
            contentScale = ContentScale.Crop,
        )
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = "알림",
                    modifier = Modifier.size(20.dp),
                    tint = ChaekInk,
                )
            }
        }
    }
}

@Composable
private fun TrendingSection(section: FeedSectionUiModel.TrendingBooks) {
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
                onSelect = { selectedIndex = index },
                x = x,
                y = y,
                scaleX = scaleX,
                scaleY = scaleY,
                rotation = rotation,
            )
        }

        selectedBook?.let { book ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 190.dp)
                    .size(width = 150.dp, height = 38.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(6.dp),
                color = ChaekSurface,
                border = BorderStroke(1.dp, ChaekBorderSoft),
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
//                    Spacer(Modifier.width(8.dp))
//                    Text("↗", color = ChaekAccent, fontSize = 14.sp)
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
    onSelect: () -> Unit,
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
            .clickable(role = Role.Button, onClick = onSelect),
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
    quotes: List<QuoteCardUiModel>,
    overlapped: List<OverlappedCardUiModel>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "최근 감상들",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 25.sp,
                lineHeight = 30.sp,
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
                    avatar = R.drawable.avatar_kim,
                )
            }
            items(overlapped, key = { it.bookId.value }) { card ->
                ReflectionCard(
                    title = card.title,
                    coverId = card.coverId,
                    authorLabel = card.authorLabel,
                    excerpt = card.excerpt,
                    replyLabel = card.replyLabel,
                    avatar = R.drawable.avatar_yoon,
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
    @DrawableRes
    avatar: Int,
) {
    Surface(
        modifier = Modifier.size(width = 318.dp, height = 184.dp),
        shape = RoundedCornerShape(12.dp),
        color = ChaekSurface,
        border = BorderStroke(1.dp, ChaekBorderSoft),
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
                        color = ChaekInkSecondary,
                        fontSize = 20.sp,
                        lineHeight = 20.sp,
                    )
                }
                AuthorLine(authorLabel, avatar, imageSize = 20.dp)
                Text(
                    "“$excerpt”",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ChaekInkSecondary,
                    )
                    Text(
                        replyLabel,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ChaekInkSecondary,
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

@Composable
private fun InvisibleCitiesCover(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFF0F0EC))
            .border(.5.dp, Color.Black.copy(alpha = .12f), RoundedCornerShape(2.dp)),
    ) {
        Text(
            "LE CITTÀ\nINVISIBILI",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 7.dp, top = 7.dp),
            color = Color(0xFF171717),
            fontSize = 8.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        Box(
            Modifier
                .offset(x = 27.dp, y = 51.dp)
                .size(width = 67.dp, height = 73.dp)
                .graphicsLayer { rotationZ = 8f }
                .background(Color(0xFF252525)),
        )
        Text(
            "看不見的城市",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 8.dp),
            color = Color(0xFF252525),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AuthorLine(
    label: String,
    @DrawableRes
    avatar: Int,
    imageSize: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(avatar),
            contentDescription = null,
            modifier = Modifier
                .size(imageSize)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = ChaekInkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Cover(coverId: String, title: String, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(coverResource(coverId)),
        contentDescription = "$title 표지",
        modifier = modifier
            .border(.5.dp, Color.Black.copy(alpha = .2f), RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp)),
        contentScale = ContentScale.Crop,
    )
}

private fun AppError.message(): String =
    when (this) {
        AppError.Network -> "네트워크 연결을 확인한 뒤 다시 시도해 주세요."
        AppError.NotFound -> "홈 피드를 찾을 수 없어요."
        AppError.Unauthorized -> "로그인이 필요한 요청이에요."
        AppError.Unknown -> "잠시 후 다시 시도해 주세요."
    }

@DrawableRes
private fun coverResource(coverId: String): Int =
    when (coverId) {
        "cover-01" -> R.drawable.cover_01
        "cover-02" -> R.drawable.cover_02
        "cover-03" -> R.drawable.cover_03
        "cover-04" -> R.drawable.cover_04
        "cover-05" -> R.drawable.cover_05
        "cover-06" -> R.drawable.cover_06
        "cover-07" -> R.drawable.cover_07
        "cover-08" -> R.drawable.cover_08
        "cover-09" -> R.drawable.cover_09
        "cover-10" -> R.drawable.cover_10
        "cover-11" -> R.drawable.cover_11
        "cover-12" -> R.drawable.cover_12
        "cover-13" -> R.drawable.cover_13
        "cover-14" -> R.drawable.cover_14
        "cover-15" -> R.drawable.cover_15
        "cover-16" -> R.drawable.cover_16
        "cover-17" -> R.drawable.cover_17
        "cover-18" -> R.drawable.cover_18
        "cover-19" -> R.drawable.cover_19
        "cover-20" -> R.drawable.cover_20
        else -> R.drawable.app_logo_square
    }

@Preview(name = "홈 · 읽는 책 있음", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeWithReadingBookPreview() {
    HomePreview(
        ReadingBookUiModel(
            title = "역병",
            coverId = "cover-17",
            currentPage = 132,
            totalPages = 320,
        ),
    )
}

@Preview(name = "홈 · 읽는 책 없음", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomeWithoutReadingBookPreview() {
    HomePreview(readingBook = null)
}

@Composable
private fun HomePreview(readingBook: ReadingBookUiModel?) {
    val trendingBooks = listOf(
        "cover-18" to "보이지 않는 도시",
        "cover-17" to "역병",
        "cover-13" to "마션",
        "cover-14" to "침묵하는 다수",
        "cover-15" to "장일장진",
        "cover-16" to "여름의 문장들",
    ).map { (coverId, title) ->
        TrendingBookUiModel(BookId(coverId), title, coverId, "")
    }
    val state = HomeUiState.Content(
        sections = listOf(
            FeedSectionUiModel.TrendingBooks(trendingBooks, ""),
            FeedSectionUiModel.RecentQuotes(
                title = "최근 감상들",
                cards = listOf(
                    QuoteCardUiModel(
                        noteId = NoteId("preview-note"),
                        bookId = BookId("cover-18"),
                        bookTitle = "보이지 않는 도시",
                        coverId = "cover-18",
                        authorLabel = "다정한 참새 · 4분 전",
                        quoteText = "도시는 기억으로 만들어진다는 문장에서 오래 멈췄다.",
                        replyLabel = "답글 12",
                    ),
                ),
            ),
            FeedSectionUiModel.OverlappedBooks(
                title = "밑줄이 겹친 책",
                cards = listOf(
                    OverlappedCardUiModel(
                        bookId = BookId("cover-17"),
                        title = "역병",
                        coverId = "cover-17",
                        noteCountLabel = "감상 96",
                        authorLabel = "느긋한 참새 · 오늘",
                        excerpt = "서로를 돌보는 일은 매일의 선택이었다.",
                        replyLabel = "답글 28",
                    ),
                ),
            ),
        ),
        guestBanner = null,
        readingBook = readingBook,
    )

    ChaekchaekTheme(darkTheme = false) {
        HomeContent(state, onSearchBook = {}, modifier = Modifier.fillMaxSize())
    }
}
