package com.chaekchaek.app.ui.bookdetail

/** 서버 계약과 독립적으로 주입하는 책별 질문과 참고 자료. null은 자료가 없음을 뜻한다. */
data class BookQuestionContent(
    val id: String,
    val markdown: String,
    val plotMarkdown: String? = null,
    val excerptMarkdown: String? = null,
) {
    init {
        require(id.isNotBlank()) { "질문 ID는 비어 있을 수 없습니다." }
        require(markdown.isNotBlank()) { "질문은 비어 있을 수 없습니다." }
    }

    val availableTabs: List<BookQuestionReferenceTab>
        get() = BookQuestionReferenceTab.entries.filter { referenceMarkdown(it) != null }

    fun referenceMarkdown(tab: BookQuestionReferenceTab): String? = when (tab) {
        BookQuestionReferenceTab.Plot -> plotMarkdown
        BookQuestionReferenceTab.Excerpt -> excerptMarkdown
    }?.takeIf(String::isNotBlank)

    fun selectedReferenceTab(preferred: BookQuestionReferenceTab? = null): BookQuestionReferenceTab? =
        preferred?.takeIf { referenceMarkdown(it) != null } ?: availableTabs.firstOrNull()
}

enum class BookQuestionReferenceTab { Plot, Excerpt }

/** 가용 높이와 같은 네이티브 단위를 반환하고, 작은 화면에서는 최소 높이보다 가용 공간을 우선한다. */
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

/** 별점은 시안용 초안 값이다. null은 미선택이며 0f는 실제 0점으로 구분한다. */
data class BookDetailReviewDraft(
    val content: String,
    val quote: String? = null,
    val isSpoiler: Boolean = false,
    val rating: Float? = null,
) {
    init {
        require(rating == null || (rating.isFinite() && rating in 0f..5f && rating * 2f % 1f == 0f)) {
            "별점 초안은 0부터 5까지 0.5 단위이거나 미선택이어야 합니다."
        }
    }
}

/** HTML 시안에서 승인한 내 반응 선택지. 서버 집계나 저장 계약을 뜻하지 않는다. */
enum class BookQuestionReaction(val emoji: String, val label: String) {
    ThumbsUp("👍", "좋아요"),
    Heart("❤️", "마음에 남아요"),
    Thinking("🤔", "생각하게 돼요"),
    Relaxed("😌", "편안해요"),
    Moved("🥲", "뭉클해요"),
    Sparkles("✨", "인상적이에요"),
}
