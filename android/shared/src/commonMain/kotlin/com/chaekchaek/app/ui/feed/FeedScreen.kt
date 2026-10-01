package com.chaekchaek.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_comment
import chaekchaek.shared.generated.resources.ic_heart_filled
import chaekchaek.shared.generated.resources.ic_heart_outline
import com.chaekchaek.app.presentation.home.FeedSectionUiModel
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.ui.RemoteBookImage
import com.chaekchaek.app.ui.common.BrandHeader
import com.chaekchaek.app.ui.common.HomeLoadErrorContent
import com.chaekchaek.app.ui.home.BookDetailTarget
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.feedBookShadow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun FeedScreen(
    homeViewModel: HomeViewModel,
    onBookClick: (BookDetailTarget) -> Unit,
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by homeViewModel.uiState.collectAsState()
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BrandHeader(onProfileClick)
        when (val current = state) {
            HomeUiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            HomeUiState.Empty -> FeedEmptyContent(Modifier.weight(1f))
            is HomeUiState.Failure -> HomeLoadErrorContent(
                error = current.error,
                retry = homeViewModel::retry,
                modifier = Modifier.weight(1f),
            )
            is HomeUiState.Content -> FeedContent(
                reflections = current.sections
                    .filterIsInstance<FeedSectionUiModel.RecentQuotes>()
                    .flatMap { it.cards },
                onBookClick = onBookClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeedEmptyContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("아직 도착한 감상이 없어요.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FeedContent(
    reflections: List<QuoteCardUiModel>,
    onBookClick: (BookDetailTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reflections.isEmpty()) {
        FeedEmptyContent(modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 24.dp),
        ) {
            itemsIndexed(reflections, key = { _, item -> item.noteId.value }) { _, reflection ->
                HorizontalDivider(color = ChaekBorderSoft)
                FeedReflectionArticle(
                    reflection = reflection,
                    onOpenBook = { onBookClick(reflection.toBookDetailTarget()) },
                )
            }
        }
    }
}

@Composable
private fun FeedReflectionArticle(reflection: QuoteCardUiModel, onOpenBook: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 330.dp)
            .padding(top = 22.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(width = 64.dp, height = 110.dp)
                .feedBookShadow()
                .clip(RoundedCornerShape(2.dp))
                .clickable(role = Role.Button, onClick = onOpenBook),
        ) {
            RemoteBookImage(reflection.coverId, "${reflection.bookTitle} 표지", Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                reflection.bookTitle,
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenBook),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFE1E1E5)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        reflection.authorName.take(1),
                        color = ChaekInkSecondary,
                        fontSize = 9.sp,
                        lineHeight = 14.4.sp,
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        reflection.authorName,
                        color = ChaekInkSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 16.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        reflection.timeLabel,
                        color = ChaekInkSecondary.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 16.sp),
                        maxLines = 1,
                    )
                }
            }
            Text(
                reflection.quoteText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 28.8.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            reflection.quote?.takeIf { it.isNotBlank() }?.let { quote ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "“",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        lineHeight = 32.sp,
                    )
                    Text(
                        quote,
                        modifier = Modifier.padding(start = 8.dp, top = 5.dp),
                        color = ChaekInkSecondary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 25.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FeedAction(
                    icon = if (reflection.likedByMe) Res.drawable.ic_heart_filled else Res.drawable.ic_heart_outline,
                    label = "좋아요 ${reflection.likeCount}",
                )
                Spacer(Modifier.size(18.dp))
                FeedAction(Res.drawable.ic_comment, "답글 ${reflection.replyCount}")
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "↗",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = ChaekIconFontFamily(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedAction(icon: DrawableResource, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(19.dp), tint = ChaekInkSecondary)
        Text(
            label,
            color = ChaekInkSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        )
    }
}

private fun QuoteCardUiModel.toBookDetailTarget() = BookDetailTarget(
    id = isbn13.ifBlank { bookId.value },
    isbn13 = isbn13,
    bookId = bookId.value.toLongOrNull(),
    title = bookTitle,
    coverId = coverId,
    coverUrl = coverId.takeIf { it.startsWith("https://") }.orEmpty(),
)
