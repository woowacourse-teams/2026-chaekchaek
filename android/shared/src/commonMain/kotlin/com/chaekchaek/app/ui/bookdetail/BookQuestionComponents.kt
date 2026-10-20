package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.question_emoji_bubble
import com.chaekchaek.app.ui.theme.*
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BookQuestionSection(
    content: BookQuestionContent,
    reaction: BookQuestionReaction?,
    onReferenceOpen: () -> Unit,
    onReviewOpen: () -> Unit,
    onReaction: (BookQuestionReaction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 20.dp).testTag("book_question"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("여운 한 줄", fontSize = 15.sp, lineHeight = 22.5.sp, color = ChaekInk)
        BookQuestionMarkdown(content.markdown, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(ChaekSurfaceMuted).padding(16.dp), textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp))
        if (content.availableTabs.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                .border(1.dp, ChaekBorder, RoundedCornerShape(12.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onReferenceOpen)
                .testTag("question_reference_open").padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("줄거리와 발췌 보기", Modifier.weight(1f), fontSize = 14.sp, lineHeight = 21.sp, color = ChaekInk)
                Text("↗", fontFamily = ChaekIconFontFamily(), fontSize = 16.sp, color = ChaekInk)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.heightIn(min = 44.dp)
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onReviewOpen)
                .testTag("question_review_open"), contentAlignment = Alignment.Center) {
                Row(Modifier.clip(RoundedCornerShape(8.dp)).background(ChaekInk).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("감상 쓰기", fontSize = 13.sp, lineHeight = 19.5.sp, color = ChaekSurface)
                    Text("↗", fontFamily = ChaekIconFontFamily(), fontSize = 16.sp, color = ChaekAccent)
                }
            }
            BookQuestionReactions(content.id, reaction, onReaction)

        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun BookQuestionReactions(
    questionId: String,
    reaction: BookQuestionReaction?,
    onReaction: (BookQuestionReaction?) -> Unit,
) {
    var expanded by rememberSaveable(questionId) { mutableStateOf(false) }
    val anchorFocus = remember { FocusRequester() }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val margin = with(density) { 12.dp.roundToPx() }
    val popupMaxWidth = with(density) { (windowInfo.containerSize.width - margin * 2).coerceAtLeast(44).toDp() }
    val positionProvider = remember(margin) { BookQuestionReactionPositionProvider(margin) }
    fun close(restoreFocus: Boolean) {
        expanded = false
        if (restoreFocus) anchorFocus.requestFocus()
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = expanded,
        onBackCompleted = { close(restoreFocus = true) },
    )
    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            reaction?.let { selected ->
                Box(Modifier.size(44.dp)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                        onClickLabel = "반응 취소", onClick = { onReaction(null); close(restoreFocus = true) })
                    .semantics {
                        contentDescription = "${selected.label} 반응 취소"
                        stateDescription = "내 반응 1개, 선택됨"
                    }.testTag("question_reaction_selected"), contentAlignment = Alignment.Center) {
                    Row(Modifier.size(40.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(ChaekSurfaceMuted).clearAndSetSemantics {},
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(selected.emoji, fontFamily = ChaekIconFontFamily(), fontSize = 16.sp, lineHeight = 20.sp)
                        Text("1", fontSize = 12.sp, lineHeight = 19.5.sp, color = ChaekInk)
                    }
                }
            }
            Box(Modifier.size(44.dp).focusRequester(anchorFocus)
                .onPreviewKeyEvent { event ->
                    if (expanded && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        close(restoreFocus = true)
                        true
                    } else false
                }
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                    onClick = { expanded = !expanded; anchorFocus.requestFocus() })
                .semantics {
                    contentDescription = "질문 이모지 반응"
                    stateDescription = if (expanded) "선택창 열림" else "선택창 닫힘"
                }.testTag("question_reaction"), contentAlignment = Alignment.Center) {
                Box(Modifier.size(32.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(ChaekSurfaceMuted),
                    contentAlignment = Alignment.Center) {
                    Image(painterResource(Res.drawable.question_emoji_bubble), contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (expanded) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { close(restoreFocus = false) },
                properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = true),
            ) {
                FlowRow(
                    Modifier.widthIn(max = popupMaxWidth).shadow(4.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)).background(ChaekSurface)
                        .border(1.dp, ChaekBorder, RoundedCornerShape(12.dp)).padding(5.dp)
                        .testTag("question_reaction_picker")
                        .semantics { paneTitle = "이모지 선택" },
                    maxItemsInEachRow = 6,
                ) {
                    BookQuestionReaction.entries.forEach { choice ->
                        Box(Modifier.size(44.dp)
                            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button,
                                onClick = { onReaction(choice); close(restoreFocus = true) })
                            .semantics {
                                contentDescription = choice.label
                                selected = choice == reaction
                            }.testTag("question_reaction_choice_${choice.name}"), contentAlignment = Alignment.Center) {
                            Text(choice.emoji, modifier = Modifier.clearAndSetSemantics {}, fontFamily = ChaekIconFontFamily(), fontSize = 18.sp, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

private class BookQuestionReactionPositionProvider(private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val below = anchorBounds.bottom
        val y = if (below <= maxY) below else anchorBounds.top - popupContentSize.height
        return IntOffset((anchorBounds.right - popupContentSize.width).coerceIn(margin, maxX), y.coerceIn(margin, maxY))
    }
}

/** 자신의 bounds 안에서만 입력을 받는다. 바깥 감상 목록은 그대로 스크롤할 수 있다. */
@Composable
internal fun BookQuestionOverlay(
    content: BookQuestionContent,
    selectedTab: BookQuestionReferenceTab,
    onTabSelect: (BookQuestionReferenceTab) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.shadow(12.dp, RoundedCornerShape(22.dp))
        .clip(RoundedCornerShape(22.dp)).background(ChaekSurface)
        .border(1.dp, ChaekBorder, RoundedCornerShape(22.dp))
        // 패널의 빈 여백도 입력 경계로 삼아 뒤 목록으로 터치가 통과하지 않게 한다.
        .pointerInput(Unit) {}
        .testTag("question_reference_overlay").padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BookQuestionMarkdown(content.markdown, Modifier.weight(1f).heightIn(max = 120.dp)
                .clip(RoundedCornerShape(12.dp)).background(ChaekSurfaceMuted)
                .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp))
            Box(Modifier.size(44.dp).clickable(remember { MutableInteractionSource() }, indication = null,
                role = Role.Button, onClickLabel = "참고 자료 닫기", onClick = onClose)
                .semantics { contentDescription = "참고 자료 닫기" }.testTag("question_reference_close"), contentAlignment = Alignment.Center) {
                Text("×", fontFamily = ChaekIconFontFamily(), fontSize = 20.sp, color = ChaekInk)
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ChaekSurfaceMuted)) {
            content.availableTabs.forEach { tab ->
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (tab == selectedTab) ChaekSurface else ChaekSurfaceMuted)
                    .selectable(tab == selectedTab, remember { MutableInteractionSource() }, indication = null,
                        role = Role.Tab, onClick = { onTabSelect(tab) })
                    .testTag("question_tab_${tab.name}"), contentAlignment = Alignment.Center) {
                    Text(if (tab == BookQuestionReferenceTab.Plot) "줄거리" else "본문 발췌", fontSize = 14.sp,
                        lineHeight = 22.sp, color = if (tab == selectedTab) ChaekInk else ChaekInkSecondary)
                }
            }
        }
        key(content.id, selectedTab) {
            BookQuestionMarkdown(content.referenceMarkdown(selectedTab).orEmpty(),
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).testTag("question_reference_body"))
        }
    }
}
