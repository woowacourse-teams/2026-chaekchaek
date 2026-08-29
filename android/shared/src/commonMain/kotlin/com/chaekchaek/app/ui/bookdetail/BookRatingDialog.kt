package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_close
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekAccentSoft
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekInkTertiary
import com.chaekchaek.app.ui.theme.ChaekSurface
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BookRatingDialog(
    currentBookId: String,
    initialRating: Rating?,
    recentRatings: List<RatedBookUiModel>,
    onDismiss: () -> Unit,
    onSave: (Rating) -> Unit,
) {
    var selected by remember(initialRating) { mutableStateOf(initialRating ?: Rating.ofHalfStars(8)) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(10.dp),
            color = ChaekSurface,
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("이 책에 별점 매기기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Surface(onClick = onDismiss, color = Color.Transparent, modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_close),
                                contentDescription = "별점 창 닫기",
                                modifier = Modifier.size(16.dp),
                                tint = ChaekInk,
                            )
                        }
                    }
                }
                Text(
                    "최근 남긴 별점을 확인하고 새 별점을 선택하세요.",
                    color = ChaekInkSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("내 평점 기록", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${recentRatings.size}회", color = ChaekInkSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                RecentRatings(recentRatings, currentBookId)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = ChaekBorderSoft)
                Spacer(Modifier.height(14.dp))
                Text(
                    "새 별점",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                RatingSelector(selected = selected, onSelect = { selected = it })
                Text(
                    RatingDialogRules.label(selected),
                    modifier = Modifier.fillMaxWidth(),
                    color = ChaekInkSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = ChaekSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("취소", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        onClick = { onSave(selected) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = ChaekInk,
                        contentColor = ChaekSurface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("별점 저장", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRatings(ratings: List<RatedBookUiModel>, currentBookId: String) {
    if (ratings.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
            Text("아직 남긴 별점이 없어요", color = ChaekInkTertiary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(92.dp).border(1.dp, ChaekBorderSoft, RoundedCornerShape(2.dp)),
    ) {
        ratings.forEachIndexed { index, item ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(if (item.bookId == currentBookId) ChaekAccentSoft else ChaekSurface)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    item.rating.score.toString(),
                    color = ChaekAccent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    item.title,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.ratedAtLabel,
                    color = ChaekInkTertiary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                )
            }
            if (index < ratings.lastIndex) Box(Modifier.width(1.dp).fillMaxHeight().background(ChaekBorderSoft))
        }
    }
}

@Composable
private fun RatingSelector(selected: Rating, onSelect: (Rating) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(5) { starIndex ->
            val filledHalfStars = (selected.halfStars - starIndex * 2).coerceIn(0, 2)
            Box(modifier = Modifier.width(40.dp).height(48.dp), contentAlignment = Alignment.Center) {
                Text("★", modifier = Modifier.width(40.dp), color = ChaekBorderSoft, fontSize = 34.sp, textAlign = TextAlign.Center)
                if (filledHalfStars > 0) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                            .fillMaxWidth(filledHalfStars / 2f).clipToBounds(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("★", modifier = Modifier.width(40.dp), color = ChaekAccent, fontSize = 34.sp, textAlign = TextAlign.Center)
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    repeat(2) { halfIndex ->
                        val rating = RatingDialogRules.ratingAtSlot(starIndex * 2 + halfIndex)
                        Box(
                            Modifier.weight(1f).fillMaxHeight().semantics {
                                contentDescription = "${rating.score}점"
                                this.selected = selected == rating
                                role = Role.RadioButton
                                onClick {
                                    onSelect(rating)
                                    true
                                }
                            }.pointerInput(rating) { detectTapGestures { onSelect(rating) } },
                        )
                    }
                }
            }
        }
    }
}
