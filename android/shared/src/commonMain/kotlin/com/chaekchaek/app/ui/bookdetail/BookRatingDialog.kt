package com.chaekchaek.app.ui.bookdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.domain.rating.Rating
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekInkTertiary
import com.chaekchaek.app.ui.theme.ChaekSurface

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
        title = {
            Text(
                if (initialRating == null) "이 책에 별점 매기기" else "이 책의 별점 수정하기",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    "선택한 별점과 내 평점 기록을 비교해 보세요.",
                    color = ChaekInkSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("내 평점 기록", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${comparisonRatings.size}권", color = ChaekInkSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
                RatingComparisons(comparisonRatings)
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = ChaekBorderSoft)
                Spacer(Modifier.height(14.dp))
                Text(
                    if (initialRating == null) "새 별점" else "별점 수정",
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
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(6.dp)) {
                Text("취소", style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text("별점 저장", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun RatingComparisons(ratings: List<RatingComparisonBookUiModel>) {
    if (ratings.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.Center) {
            Text("비교할 평점 기록이 없어요", color = ChaekInkTertiary, style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(92.dp).border(1.dp, ChaekBorderSoft, RoundedCornerShape(2.dp)),
    ) {
        ratings.forEachIndexed { index, item ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(ChaekSurface)
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    item.rating.toString(),
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
                    Text(
                        "★",
                        modifier = Modifier.width(40.dp).drawWithContent {
                            clipRect(right = size.width * filledHalfStars / 2f) {
                                this@drawWithContent.drawContent()
                            }
                        },
                        color = ChaekAccent,
                        fontSize = 34.sp,
                        textAlign = TextAlign.Center,
                    )
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
