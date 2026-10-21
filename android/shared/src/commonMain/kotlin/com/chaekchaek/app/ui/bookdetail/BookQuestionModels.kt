package com.chaekchaek.app.ui.bookdetail

/** 서버 계약과 분리한 책 상세 질문 표시 모델. */
data class BookQuestionUiModel(
    val id: String,
    val questionMarkdown: String,
    val synopsisMarkdown: String,
    val excerptMarkdown: String,
)

enum class BookQuestionReferenceTab {
    Synopsis,
    Excerpt,
}
