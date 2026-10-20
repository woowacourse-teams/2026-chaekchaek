package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookReview

/** Debug 실행 옵션에서만 진입한다. 실제 도서 저장소와 연결하지 않는 검증용 시안이다. */
@Composable
fun BookQuestionPreviewScreen(
    modifier: Modifier = Modifier,
    questionVisible: Boolean = true,
    onReviewPreviewed: (BookDetailReviewDraft) -> Unit = {},
) {
    var reactionName by rememberSaveable { mutableStateOf<String?>(null) }
    val reaction = BookQuestionReaction.entries.firstOrNull { it.name == reactionName }
    var previewSubmitted by rememberSaveable { mutableStateOf(false) }
    val state = remember {
        BookDetailUiState(
            book = BookDetailArgs(id = "question-a-preview", title = "어린 왕자", creator = "앙투안 드 생텍쥐페리", publisher = "열린책들", year = "2015", category = "소설", totalPages = 136),
            detail = BookDetail(bookId = null, isbn13 = "", title = "어린 왕자", authors = listOf("앙투안 드 생텍쥐페리"), translators = listOf("황현산"), publisher = "열린책들", category = "소설", publishedDate = "2015", totalPages = 136, coverImageUrl = "", commentCount = 8, averageRating = 4.4, ratingCount = 12, myRecord = null),
            reviews = List(8) { index -> BookReview(
                reviewId = index.toLong() + 1, content = "서로에게 특별해지는 데에는 시간이 필요하다는 말이 오래 남았습니다. 익숙한 문장을 다시 읽으며 내 곁의 사람들을 떠올렸어요. ${index + 1}",
                quote = "네 장미꽃을 그토록 소중하게 만든 건 그 꽃을 위해 네가 소비한 시간이야.", chapter = null, currentPage = null,
                createdAt = "2026-10-20T12:00:00", authorName = "독자 ${index + 1}", anonymous = false, replyCount = 0, likeCount = index,
            ) },
            reviewCount = 8,
            signedIn = true,
        )
    }
    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        if (previewSubmitted) Text("시안에서 입력을 확인했어요.", Modifier.padding(horizontal = 20.dp).testTag("question_preview_confirmation"))
        BookDetailScreen(
            state = state,
            onBack = {},
            modifier = Modifier.weight(1f),
            questionContent = if (questionVisible) BookQuestionPreviewContent else null,
            questionReaction = reaction,
            onQuestionReaction = { reactionName = it?.name },
            onPreviewReviewSave = { draft -> onReviewPreviewed(draft); previewSubmitted = true },
            coverContent = { _, imageModifier ->
                Column(imageModifier.background(ChaekSurfaceMuted).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("어린 왕자", fontSize = 21.sp, lineHeight = 30.sp)
                    Text("앙투안 드 생텍쥐페리", fontSize = 10.sp, lineHeight = 16.sp)
                }
            },
        )
    }
}

internal val BookQuestionPreviewContent = BookQuestionContent(
    id = "preview-prince-question",
    markdown = "누군가에게 시간을 들여 특별해진 경험이 있나요?",
    plotMarkdown = """
        ## 어린 왕자와 여우

        어린 왕자는 여행 중 여우를 만납니다. 여우는 **관계를 맺는 일**을 이야기하며, 함께 보내는 시간이 서로를 특별하게 만든다고 말합니다.

        > 익숙해지는 시간은 누군가를 알아가는 시간이기도 합니다.

        어린 왕자는 자신이 돌보던 장미를 떠올립니다. 물을 주고, 바람을 막아 주고, 이야기를 들어 주며 보낸 시간이 장미를 소중하게 만들었다는 것을 이해합니다.

        질문을 읽으며 자신에게 특별해진 사람이나 일상을 천천히 떠올려 보세요.

        - 함께 보낸 시간
        - 기다렸던 순간
        - 기억에 남은 말

        이 자료는 화면 검증용 예시입니다.
    """.trimIndent(),
    excerptMarkdown = """
        > “길들인다는 게 무슨 뜻이야?”

        “관계를 맺는다는 뜻이지.” 여우가 말했다.

        “지금 너는 내게 수많은 다른 소년들과 조금도 다를 바 없는 작은 소년일 뿐이야. 그래서 나는 네가 필요하지 않아. 너도 내가 필요하지 않고.”

        “하지만 네가 나를 길들이면 우리는 서로를 필요로 하게 돼. 너는 내게 세상에서 하나뿐인 사람이 되고, 나는 네게 세상에서 하나뿐인 여우가 되는 거야.”

        어린 왕자는 장미를 생각했습니다.

        **“네 장미꽃을 그토록 소중하게 만든 건 그 꽃을 위해 네가 소비한 시간이야.”**

        이 자료는 화면 검증용 예시입니다.
    """.trimIndent(),
)
