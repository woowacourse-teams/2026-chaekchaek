package com.chaekchaek.app.ui.bookdetail

import com.chaekchaek.app.data.remote.BookQuestion

/** 서버 계약과 분리한 책 상세 질문 표시 모델. */
data class BookQuestionUiModel(
    val id: String,
    val questionMarkdown: String,
    val synopsisMarkdown: String,
    val excerptMarkdown: String,
) {
    val referenceTabs: List<BookQuestionReferenceTab>
        get() = buildList {
            if (synopsisMarkdown.isNotBlank()) add(BookQuestionReferenceTab.Synopsis)
            if (excerptMarkdown.isNotBlank()) add(BookQuestionReferenceTab.Excerpt)
        }
}

fun BookQuestion.toUiModel(isbn13: String) = BookQuestionUiModel(
    id = isbn13,
    questionMarkdown = question,
    synopsisMarkdown = plot.orEmpty(),
    excerptMarkdown = extract.orEmpty(),
)

enum class BookQuestionReferenceTab {
    Synopsis,
    Excerpt,
}
