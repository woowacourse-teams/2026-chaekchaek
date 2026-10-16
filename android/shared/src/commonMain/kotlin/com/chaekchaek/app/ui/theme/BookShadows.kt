package com.chaekchaek.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

// Figma 805:104-114. 콜라주 전체가 아니라 각 표지에 적용한다.
internal fun Modifier.collageBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(20.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(0.dp, 12.dp)))
        .dropShadow(RoundedCornerShape(2.dp), Shadow(3.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(1.dp, 2.dp)))

// Figma 805:35. 최근 감상 카드 안의 표지 전용.
internal fun Modifier.recentBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(9.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(2.dp, 5.dp)))

// Figma 807:22, 807:67, 807:111. 피드 행의 책 표지 전용.
internal fun Modifier.feedBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(8.dp, Color.Black.copy(alpha = .09f), offset = DpOffset(1.dp, 4.dp)))

// Figma 808:64, 808:79, 808:94. 발견의 인기 책 표지 전용.
internal fun Modifier.discoverPopularBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(13.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(3.dp, 8.dp)))

// Figma 808:124, 808:145. 발견의 최근 감상 표지 전용.
internal fun Modifier.discoverReflectionBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(9.dp, Color.Black.copy(alpha = .07f), offset = DpOffset(2.dp, 5.dp)))

// Figma 809:56, 809:86, 809:116. 서재 목록 표지 전용.
internal fun Modifier.libraryBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(9.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(2.dp, 5.dp)))

// Figma 803:15. 책 상세 상단 표지 전용.
internal fun Modifier.detailBookShadow(): Modifier =
    dropShadow(RoundedCornerShape(2.dp), Shadow(16.dp, Color.Black.copy(alpha = .13f), offset = DpOffset(4.dp, 10.dp)))

// Figma 805:169. 작은 표지가 아니라 이어서 읽기 카드 전체에 적용한다.
internal fun Modifier.continueReadingShadow(): Modifier =
    dropShadow(RoundedCornerShape(20.dp), Shadow(15.dp, Color.Black.copy(alpha = .18f), offset = DpOffset(0.dp, 12.dp)))
