package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekInkTertiary

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.stateDescription
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.rating_star_filled
import org.jetbrains.compose.resources.painterResource
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted

@Composable
internal fun BookRatingDialog(
    initialRating: Rating?,
    comparisonRatings: List<RatingComparisonBookUiModel>,
    onCriterionChange: (Rating) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Rating) -> Unit,
) {
    var selected by remember(initialRating) { mutableStateOf(initialRating ?: Rating.ofHalfStars(8)) }
    LaunchedEffect(selected) { onCriterionChange(selected) }
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRating == null) "이 책에 별점 매기기" else "이 책의 별점 수정하기", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("별을 누르거나 +, - 버튼으로 0.5점씩 조절해 보세요.", color = ChaekInkSecondary, style = MaterialTheme.typography.bodySmall)
                Text(if (initialRating == null) "새 별점" else "별점 수정", style = MaterialTheme.typography.labelMedium)
                RatingSelector(selected = selected, onSelect = { selected = it })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RatingStepButton("-", "0.5점 낮추기", selected.halfStars > 1) {
                        selected = Rating.ofHalfStars(selected.halfStars - 1)
                    }
                    Text(
                        selected.score.toString(),
                        modifier = Modifier.weight(1f).semantics { stateDescription = "${selected.score}점" },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    RatingStepButton("+", "0.5점 높이기", selected.halfStars < 10) {
                        selected = Rating.ofHalfStars(selected.halfStars + 1)
                    }
                }
                HorizontalDivider(color = ChaekBorderSoft)
                RatingComparisons(comparisonRatings)
            }
        },
        dismissButton = { ChaekOverlayButton("취소", onDismiss, secondary = true) },
        confirmButton = { ChaekOverlayButton("별점 저장", { onSave(selected) }) },
    )
}

@Composable
private fun RatingStepButton(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { contentDescription = label },
        shape = ChaekOverlayTokens.cardShape,
        color = ChaekSurfaceMuted,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, fontFamily = ChaekIconFontFamily(), style = MaterialTheme.typography.titleMedium,
                color = if (enabled) ChaekInk else ChaekInkTertiary)
        }
    }
}

@Composable
private fun RatingComparisons(ratings: List<RatingComparisonBookUiModel>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("내가 이전에 평가한 작품", style = MaterialTheme.typography.bodySmall)
        if (ratings.isEmpty()) {
            Box(Modifier.fillMaxWidth().background(ChaekSurfaceMuted, ChaekOverlayTokens.cardShape).padding(16.dp)) {
                Text("비교할 평점 기록이 없어요", color = ChaekInkSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            // 비교 API는 lower/current/higher 세 기록만 반환한다. 빈 칸에 샘플을 채우지 않는다.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val item = ratings.getOrNull(index)
                    if (item == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        Column(
                            Modifier.weight(1f).heightIn(min = 106.dp)
                                .background(ChaekSurfaceMuted, ChaekOverlayTokens.cardShape)
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("★", fontFamily = ChaekIconFontFamily(), color = ChaekAccent, style = MaterialTheme.typography.titleMedium)
                                Text(item.rating.toString(), style = MaterialTheme.typography.titleMedium)
                            }
                            Text(item.title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSelector(selected: Rating, onSelect: (Rating) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { starIndex ->
            val filledHalfStars = (selected.halfStars - starIndex * 2).coerceIn(0, 2)
            Box(
                modifier = Modifier.weight(1f).height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Res.drawable.rating_star_filled), null,
                    modifier = Modifier.size(56.dp), tint = ChaekBorderSoft)
                if (filledHalfStars > 0) {
                    Icon(painterResource(Res.drawable.rating_star_filled), null,
                        modifier = Modifier.size(56.dp).drawWithContent {
                            clipRect(right = size.width * filledHalfStars / 2f) { this@drawWithContent.drawContent() }
                        }, tint = ChaekAccent)
                }
                Row(Modifier.fillMaxSize()) {
                    repeat(2) { halfIndex ->
                        val rating = RatingDialogRules.ratingAtSlot(starIndex * 2 + halfIndex)
                        Box(Modifier.weight(1f).fillMaxHeight().semantics {
                            contentDescription = "${rating.score}점"
                            this.selected = selected == rating
                            role = Role.RadioButton
                            onClick { onSelect(rating); true }
                        }.pointerInput(rating) { detectTapGestures { onSelect(rating) } })
                    }
                }
            }
        }
    }
}
