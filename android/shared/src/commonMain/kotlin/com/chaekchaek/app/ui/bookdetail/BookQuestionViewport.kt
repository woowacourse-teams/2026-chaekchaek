package com.chaekchaek.app.ui.bookdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility

internal fun bookQuestionOverlayHeight(availableHeight: Float): Float {
    require(availableHeight.isFinite() && availableHeight >= 0f) { "가용 높이는 0 이상의 유한한 값이어야 합니다." }
    return (availableHeight * 0.48f).coerceIn(280f, 340f).coerceAtMost(availableHeight)
}

/** 낮은 화면에서는 오른쪽 여백에서 뒤 목록을 스크롤한다. 목록 자체의 크기는 바꾸지 않는다. */
internal data class BookQuestionOverlayLayout(val height: Float, val sideScrollGutter: Boolean)

internal fun bookQuestionOverlayLayout(windowHeight: Float, reviewViewportHeight: Float): BookQuestionOverlayLayout {
    require(reviewViewportHeight.isFinite() && reviewViewportHeight >= 0f)
    val sideScrollGutter = reviewViewportHeight < 248f
    val reservedSpace = if (sideScrollGutter) 24f else 76f
    return BookQuestionOverlayLayout(
        height = bookQuestionOverlayHeight(windowHeight).coerceAtMost((reviewViewportHeight - reservedSpace).coerceAtLeast(0f)),
        sideScrollGutter = sideScrollGutter,
    )
}

/** 비모달 패널과 목록 경계에 가려진 컨트롤의 역할과 동작만 숨기고, 원래 배치는 유지한다. */
@Composable
internal fun Modifier.accessibilityWhenFullyVisible(viewport: Rect, overlay: Rect): Modifier {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val fullyVisible = !bounds.isEmpty && !viewport.isEmpty &&
        bounds.top >= viewport.top - 0.5f && bounds.bottom <= viewport.bottom + 0.5f &&
        bounds.left >= viewport.left - 0.5f && bounds.right <= viewport.right + 0.5f &&
        (overlay.isEmpty || !bounds.overlaps(overlay))
    return onGloballyPositioned { coordinates ->
        val position = coordinates.positionInWindow()
        bounds = Rect(position.x, position.y, position.x + coordinates.size.width, position.y + coordinates.size.height)
    }.then(if (fullyVisible) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() })
}

