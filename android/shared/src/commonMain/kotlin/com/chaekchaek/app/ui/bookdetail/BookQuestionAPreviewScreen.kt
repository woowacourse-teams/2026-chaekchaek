package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.data.remote.BookDetail
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted

/** 개발 도구와 DEBUG iOS 실행 인자에서만 연다. 실제 상세에 예시나 저장 성공을 공급하지 않는다. */
@Composable
fun BookQuestionAPreviewScreen(onExit: () -> Unit = {}) {
    var hasQuestion by rememberSaveable { mutableStateOf(true) }
    var signedIn by rememberSaveable { mutableStateOf(true) }
    var reaction by rememberSaveable { mutableStateOf<String?>(null) }
    var submission by remember { mutableStateOf(ReviewSubmissionState.Idle) }
    var savedRating by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedContent by rememberSaveable { mutableStateOf("") }
    var requestError by remember { mutableStateOf<String?>(null) }
    var failNext by rememberSaveable { mutableStateOf(false) }
    var showPreviewLogin by remember { mutableStateOf(false) }
    var previewNotice by rememberSaveable { mutableStateOf("") }
    val question = remember {
        BookQuestionUiModel(
            id = "development-fixture",
            questionMarkdown = "어린 왕자는 왜 **장미를 떠난 뒤에야** 그 소중함을 알게 됐을까요?",
            synopsisMarkdown = "## 장미를 떠난 어린 왕자\n\n" + (1..12).joinToString("\n\n") {
                "어린 왕자는 작은 별에 피어난 장미를 돌봅니다. 여행을 떠나 여러 만남을 겪으며 서로에게 시간을 쓴다는 의미를 배웁니다. 이 글은 화면 검증용 예시 자료입니다. ($it)"
            },
            excerptMarkdown = "> 네 장미꽃을 그토록 소중하게 만든 건 네가 그 꽃을 위해 보낸 시간이야.\n\n" +
                (1..10).joinToString("\n\n") { "**발췌 표시 예시 $it**\n\n이 자료는 마크다운 서식과 독립 스크롤을 확인하기 위한 개발 프리뷰입니다." },
        )
    }
    val book = remember { BookDetailArgs(id = "development-fixture", bookId = 1, title = "어린 왕자", creator = "앙투안 드 생텍쥐페리", publisher = "개발 프리뷰", totalPages = 200, coverId = "cover_01") }
    val reviews = remember { (1L..20L).map { id ->
        BookReview(id, "개발 프리뷰 감상 $id. 시간을 들여 읽은 장면이 오래 남습니다.", null, null, null,
            "2026-10-21T00:00:00Z", "프리뷰 독자", true, 0, 0)
    } }
    Column(Modifier.fillMaxSize()) {
        Surface(color = ChaekSurfaceMuted) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("질문 A 개발 프리뷰", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { hasQuestion = !hasQuestion }, modifier = Modifier.testTag("previewQuestionPresenceToggle")) {
                        Text(if (hasQuestion) "질문 없는 책" else "질문 있는 책")
                    }
                    TextButton(onClick = { signedIn = !signedIn }, modifier = Modifier.testTag("previewMemberToggle")) { Text(if (signedIn) "게스트" else "회원") }
                    TextButton(onClick = { failNext = !failNext }) { Text(if (failNext) "저장 실패 켜짐" else "저장 실패") }
                    TextButton(onClick = onExit) { Text("나가기") }
                }
                Text(previewNotice.ifBlank { "서버 저장 없음 | 별점: ${savedRating?.let { "${it}점" } ?: "미선택"}" }, style = MaterialTheme.typography.labelSmall)
            }
        }
        BookDetailScreen(
            modifier = Modifier.weight(1f),
            state = BookDetailUiState(
                book = book,
                detail = BookDetail(1, "", book.title, listOf(book.creator), emptyList(), book.publisher, "소설", "1943", 200, "", 20, 4.5, 10, null),
                reviews = reviews,
                reviewCount = reviews.size,
                signedIn = signedIn,
                reviewSubmissionState = submission,
                requestError = requestError,
            ),
            onBack = onExit,
            nickname = "프리뷰 독자",
            question = question.takeIf { hasQuestion },
            allowZeroReviewRating = true,
            selectedQuestionReaction = reaction,
            onQuestionReactionSelected = { reaction = it },
            onQuestionReactionClear = { reaction = null },
            onLoginRequired = { showPreviewLogin = true },
            onReviewOpen = { open -> submission = ReviewSubmissionState.Idle; open() },
            onQuestionAReviewCreate = { draft ->
                if (failNext) {
                    submission = ReviewSubmissionState.Failed
                    requestError = "개발 프리뷰의 저장 실패 예시입니다. 입력은 유지돼요."
                } else {
                    savedRating = draft.rating
                    savedContent = draft.content
                    previewNotice = "로컬 프리뷰 감상: $savedContent | 별점: ${savedRating?.let { "${it}점" } ?: "미선택"}"
                    submission = ReviewSubmissionState.Succeeded
                }
            },
            onRequestErrorShown = { requestError = null },
        )
    }
    if (showPreviewLogin) ChaekTwoActionDialog(
        onDismissRequest = { showPreviewLogin = false },
        title = { Text("프리뷰 로그인") },
        text = { Text("실제 인증 없이 회원 상태만 전환합니다. 감상 초안은 유지돼요.") },
        confirmButton = { ChaekOverlayButton("회원으로 계속", { signedIn = true; showPreviewLogin = false }) },
        dismissButton = { ChaekOverlayButton("닫기", { showPreviewLogin = false }, secondary = true) },
    )

}
