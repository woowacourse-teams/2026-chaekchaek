package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.question_reaction_bubble
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.mikepenz.markdown.model.rememberMarkdownState
import org.jetbrains.compose.resources.painterResource

// Figma 1338:2990, 1358:3683의 승인된 변수 값. 기존 테마의 border 값과 다르다.
private val QuestionDivider = Color(0xFFDDDDE1)
private val QuestionActionSurface = Color(0xFF1C1C1C)
private val QuestionActionArrow = Color(0xFFFF920F)

@Composable
fun BookQuestionCard(
    question: BookQuestionUiModel,
    selectedReaction: String?,
    onOpenReference: () -> Unit,
    onWriteReview: () -> Unit,
    onReactionClick: () -> Unit,
    onReactionClear: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "여운 한 줄",
            color = ChaekInk,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                letterSpacing = (-0.5844).sp,
            ),
        )
        QuestionMarkdown(
            markdown = question.questionMarkdown,
            modifier = Modifier.fillMaxWidth()
                .clip(ChaekOverlayTokens.cardShape)
                .background(ChaekSurfaceMuted)
                .padding(16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp)
                .clip(ChaekOverlayTokens.cardShape)
                .border(1.dp, QuestionDivider, ChaekOverlayTokens.cardShape)
                .testTag("questionReferenceOpen").questionClick(onOpenReference)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "줄거리와 발췌 보기",
                modifier = Modifier.weight(1f),
                color = ChaekInk,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.1504).sp,
                ),
            )
            Text(
                text = "↗",
                fontFamily = ChaekIconFontFamily(),
                color = ChaekInk,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.height(44.dp).questionClick(onWriteReview),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.height(32.dp)
                        .clip(ChaekOverlayTokens.cardShape)
                        .background(QuestionActionSurface)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "감상 쓰기",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.5.sp,
                            letterSpacing = (-0.4262).sp,
                        ),
                    )
                    Text(
                        text = "↗",
                        color = QuestionActionArrow,
                        fontFamily = ChaekIconFontFamily(),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                    )
                }
            }
            if (selectedReaction != null) {
                BookQuestionSelectedReaction(selectedReaction, onReactionClear)
            }
            Box(
                modifier = Modifier.size(44.dp)
                    .semantics {
                        contentDescription = "질문에 이모지 달기"
                    }
                    .testTag("questionReactionOpen")
                    .questionClick(onReactionClick),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(width = 32.dp, height = 24.dp)
                        .clip(ChaekOverlayTokens.cardShape)
                        .background(ChaekSurfaceMuted),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.question_reaction_bubble),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Figma 1416:5812. 열기와 닫기는 호출 화면이 관리하며 배경을 차단하지 않는다. */
@Composable
fun BookQuestionReactionPicker(
    selectedReaction: String?,
    onReactionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val choices = listOf(
        "👍" to "좋아요",
        "❤️" to "마음에 남아요",
        "🤔" to "생각하게 돼요",
        "😌" to "편안해요",
        "🥲" to "뭉클해요",
        "✨" to "인상적이에요",
    )
    Row(
        modifier = modifier.width(274.dp).height(54.dp)
            .clip(ChaekOverlayTokens.cardShape)
            .background(ChaekSurface)
            .border(1.dp, QuestionDivider, ChaekOverlayTokens.cardShape)
            .padding(5.dp)
            .testTag("questionReactionPicker"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        choices.forEach { (emoji, label) ->
            Box(
                modifier = Modifier.size(44.dp)
                    .semantics {
                        contentDescription = label
                        selected = selectedReaction == emoji
                    }
                    .testTag("questionReactionChoice$emoji")
                    .questionClick { onReactionSelected(emoji) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emoji,
                    fontFamily = ChaekIconFontFamily(),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun BookQuestionSelectedReaction(emoji: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp)
            .semantics {
                contentDescription = "이모지 반응 $emoji"
                stateDescription = "눌러서 내 반응 취소"
                selected = true
            }
            .testTag("questionReactionClear")
            .questionClick(onClear),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.size(width = 40.dp, height = 24.dp)
                .clip(ChaekOverlayTokens.cardShape)
                .background(ChaekSurfaceMuted),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, fontFamily = ChaekIconFontFamily(), fontSize = 16.sp, lineHeight = 20.sp)
            // HTML 시안의 내 반응 한 개다. 서버 반응 집계값을 표시하는 필드가 아니다.
            Text(
                text = "1",
                color = ChaekInk,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 19.5.sp),
            )
        }
    }
}

/** 일반 레이아웃이므로 패널 밖의 책 상세 목록은 계속 스크롤할 수 있다. */
@Composable
fun BookQuestionReferenceOverlay(
    question: BookQuestionUiModel,
    selectedTab: BookQuestionReferenceTab,
    onTabSelected: (BookQuestionReferenceTab) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val synopsisScroll = rememberSaveable(question.id, saver = ScrollState.Saver) { ScrollState(0) }
    val excerptScroll = rememberSaveable(question.id, saver = ScrollState.Saver) { ScrollState(0) }
    val synopsisMarkdown = rememberMarkdownState(question.synopsisMarkdown)
    val excerptMarkdown = rememberMarkdownState(question.excerptMarkdown)
    val scrollState = when (selectedTab) {
        BookQuestionReferenceTab.Synopsis -> synopsisScroll
        BookQuestionReferenceTab.Excerpt -> excerptScroll
    }
    Column(
        modifier = modifier.widthIn(max = 366.dp).fillMaxWidth().height(340.dp)
            .clip(ChaekOverlayTokens.containerShape)
            .background(ChaekSurface)
            .border(1.dp, QuestionDivider, ChaekOverlayTokens.containerShape)
            // 패널 여백의 탭이 아래 목록의 버튼으로 통과하지 않게 한다.
            .pointerInput(Unit) { detectTapGestures(onTap = {}) }
            .padding(1.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuestionMarkdown(
                markdown = question.questionMarkdown,
                modifier = Modifier.weight(1f)
                    .clip(ChaekOverlayTokens.cardShape)
                    .background(ChaekSurfaceMuted)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Box(
                modifier = Modifier.size(44.dp)
                    .semantics { contentDescription = "질문과 장면 닫기" }
                    .questionClick(onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "×",
                    color = ChaekInk,
                    fontFamily = ChaekIconFontFamily(),
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp)
                .clip(ChaekOverlayTokens.cardShape)
                .background(ChaekSurfaceMuted),
        ) {
            BookQuestionReferenceTab.entries.forEach { tab ->
                Box(
                    modifier = Modifier.weight(1f).height(44.dp).testTag(if (tab == BookQuestionReferenceTab.Synopsis) "questionReferenceTabSynopsis" else "questionReferenceTabExcerpt")
                        .clip(ChaekOverlayTokens.cardShape)
                        .background(if (selectedTab == tab) ChaekSurface else Color.Transparent)
                        .selectable(
                            selected = selectedTab == tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (tab == BookQuestionReferenceTab.Synopsis) "줄거리" else "본문 발췌",
                        color = if (selectedTab == tab) ChaekInk else ChaekInkSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.5004).sp,
                        ),
                    )
                }
            }
        }
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium.copy(
                color = ChaekInk,
                fontSize = 14.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.5004).sp,
            ),
        ) {
            // 다른 탭의 파싱 결과를 현재 탭의 ScrollState로 잠깐 측정하지 않는다.
            key(question.id, selectedTab) {
                BookQuestionMarkdown(
                    markdown = when (selectedTab) {
                        BookQuestionReferenceTab.Synopsis -> question.synopsisMarkdown
                        BookQuestionReferenceTab.Excerpt -> question.excerptMarkdown
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("book-question-reference-body"),
                    scrollState = scrollState,
                    parsedState = when (selectedTab) {
                        BookQuestionReferenceTab.Synopsis -> synopsisMarkdown
                        BookQuestionReferenceTab.Excerpt -> excerptMarkdown
                    },
                )
            }
        }
    }
}

@Composable
private fun QuestionMarkdown(markdown: String, modifier: Modifier = Modifier) {
    ProvideTextStyle(
        MaterialTheme.typography.bodyMedium.copy(
            color = ChaekInk,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.7625).sp,
        ),
    ) {
        BookQuestionMarkdown(markdown = markdown, modifier = modifier)
    }
}

@Composable
private fun Modifier.questionClick(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    role = Role.Button,
    onClick = onClick,
)
