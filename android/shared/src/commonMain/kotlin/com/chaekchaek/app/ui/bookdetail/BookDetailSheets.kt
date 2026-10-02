package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_eye_off
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekDanger
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch

@Composable
internal fun PageInputDialog(
    initialPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialPage.toString()) }
    val page = BookDetailInputRules.validPage(value, totalPages)
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("어디까지 읽으셨나요?", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "지금까지 읽은 쪽수를 입력하면 독서 진행률에 반영돼요.",
                    modifier = Modifier.fillMaxWidth(),
                    color = ChaekInkSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("내가 읽은 쪽수")
                    ChaekTextInput(
                        value = value,
                        onValueChange = { value = it.filter(Char::isDigit).take(7) },
                        placeholder = "0",
                        accessibilityLabel = "내가 읽은 쪽수",
                        modifier = Modifier.fillMaxWidth(),
                        height = 54,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = "쪽",
                        endText = totalPages.takeIf { it > 0 }?.let { "/ ${it}쪽" },
                        emphasized = true,
                    )
                    if (value.isNotBlank() && page == null) {
                        Text(if (totalPages > 0) "0쪽부터 ${totalPages}쪽까지 입력해 주세요." else "올바른 쪽수를 입력해 주세요.",
                            color = ChaekDanger, style = MaterialTheme.typography.bodySmall)
                    }
                    if (totalPages > 0) {
                        Text("전체 ${totalPages}쪽", color = ChaekInkSecondary, style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(progress = { ((page ?: 0).toFloat() / totalPages).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(), color = com.chaekchaek.app.ui.theme.ChaekAccent)
                    }
                }
            }
        },
        dismissButton = { ChaekOverlayButton("취소", onDismiss, secondary = true) },
        confirmButton = { ChaekOverlayButton("저장", { page?.let(onSave) }, enabled = BookDetailInputRules.canSubmitPage(page)) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReviewInputSheet(
    initialPage: Int,
    totalPages: Int,
    anonymous: Boolean,
    nickname: String,
    initialReview: BookReview? = null,
    allowReadingProgress: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (ReviewCreateRequest) -> Unit,
) {
    val initialContent = initialReview?.content.orEmpty()
    val initialQuote = initialReview?.quote.orEmpty()
    val initialChapter = initialReview?.chapter.orEmpty()
    val initialPageValue = (initialReview?.currentPage ?: initialPage.takeIf { initialReview == null })
        ?.takeIf { allowReadingProgress && it > 0 }?.toString().orEmpty()
    val initialSpoiler = initialReview?.isSpoiler == true
    val displayedAnonymously = initialReview?.anonymous ?: anonymous
    val displayedNickname = initialReview?.authorName ?: nickname
    var content by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialContent) }
    var quote by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialQuote) }
    var chapter by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialChapter) }
    var pageValue by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialPageValue) }
    var isSpoiler by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialSpoiler) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val page = if (allowReadingProgress) BookDetailInputRules.validPage(pageValue, totalPages) else null
    val canSubmit = BookDetailInputRules.canSubmitReview(content, pageValue, totalPages)
    val hasDraft = if (initialReview == null) {
        BookDetailInputRules.hasReviewDraft(content, quote, chapter, pageValue, initialPage, isSpoiler)
    } else {
        content != initialContent || quote != initialQuote || chapter != initialChapter ||
            pageValue != initialPageValue || isSpoiler != initialSpoiler
    }
    val requestDismiss = { if (hasDraft) showDiscardConfirmation = true else onDismiss() }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = ChaekSurface,
        shape = ChaekOverlayTokens.sheetShape,
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp).width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(ChaekBorder),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(title = if (initialReview == null) "감상 남기기" else "감상 수정", onDismiss = requestDismiss)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormLabel("감상", required = true)
                ChaekTextInput(
                    value = content,
                    onValueChange = { content = it.take(BookDetailInputRules.MAX_CONTENT_LENGTH) },
                    placeholder = "이 구간을 읽으며 든 생각을 남겨보세요",
                    accessibilityLabel = "감상 (필수)",
                    modifier = Modifier.fillMaxWidth(),
                    height = 120,
                    emphasized = true,
                )
                Text("감상은 반드시 입력해 주세요.", style = MaterialTheme.typography.bodySmall, color = ChaekInk)
                Text("${content.length} / ${BookDetailInputRules.MAX_CONTENT_LENGTH}", modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, color = ChaekInkSecondary)
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        .toggleable(value = isSpoiler, role = Role.Checkbox) { isSpoiler = it },
                    shape = RoundedCornerShape(6.dp),
                    color = ChaekSurfaceMuted,
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(18.dp).background(ChaekSurface, RoundedCornerShape(4.dp))
                                .border(1.dp, ChaekBorder, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSpoiler) Text(
                                "✓",
                                color = ChaekInk,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ChaekIconFontFamily(),
                            )
                        }
                        Text(
                            "스포일러",
                            modifier = Modifier.padding(start = 8.dp),
                            color = ChaekDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormLabel("인상 깊은 문구 (선택)")
                ChaekTextInput(
                    value = quote,
                    onValueChange = { quote = it.take(BookDetailInputRules.MAX_QUOTE_LENGTH) },
                    placeholder = "기억하고 싶은 문장을 옮겨 적어보세요",
                    accessibilityLabel = "인상 깊은 문구",
                    modifier = Modifier.fillMaxWidth(),
                    height = 54,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (allowReadingProgress) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FormLabel("쪽수")
                        ChaekTextInput(
                            value = pageValue,
                            onValueChange = { pageValue = it.filter(Char::isDigit).take(7) },
                            placeholder = "80",
                            accessibilityLabel = "쪽수",
                            modifier = Modifier.fillMaxWidth(),
                            height = 54,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = "쪽",
                        )
                        if (pageValue.isNotBlank() && page == null) {
                            Text(if (totalPages > 0) "0쪽부터 ${totalPages}쪽까지 입력해 주세요." else "올바른 쪽수를 입력해 주세요.",
                                color = ChaekDanger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("목차 / 챕터 (선택)")
                    ChaekTextInput(
                        value = chapter,
                        onValueChange = { chapter = it.take(BookDetailInputRules.MAX_CHAPTER_LENGTH) },
                        placeholder = "Chapter 1",
                        accessibilityLabel = "목차 또는 챕터",
                        modifier = Modifier.fillMaxWidth(),
                        height = 54,
                        singleLine = true,
                    )
                }
            }
            Surface(modifier = Modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(6.dp), color = ChaekSurfaceMuted) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.ic_eye_off),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ChaekInk,
                    )
                    Text(
                        if (displayedAnonymously) "익명" else "공개",
                        modifier = Modifier.padding(start = 7.dp),
                        color = ChaekInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "‘${displayedNickname.ifBlank { if (displayedAnonymously) "익명" else "닉네임 없음" }}’으로 표시돼요",
                        modifier = Modifier.padding(start = 7.dp),
                        color = ChaekInkSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            SheetPrimaryButton(label = if (initialReview == null) "감상 남기기" else "수정 저장", enabled = canSubmit) {
                if (!canSubmit) return@SheetPrimaryButton
                onSave(
                    ReviewCreateRequest(
                        content = content.trim(),
                        quote = quote.trim().ifEmpty { null },
                        chapter = chapter.trim().ifEmpty { null },
                        currentPage = page,
                        totalPages = totalPages.takeIf { allowReadingProgress && page != null && it > 0 },
                        isSpoiler = isSpoiler,
                    ),
                )
            }
        }
    }
    if (showDiscardConfirmation) {
        ChaekTwoActionDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(if (initialReview == null) "감상 작성을 그만둘까요?" else "감상 수정을 그만둘까요?") },
            text = { Text("작성한 내용은 저장되지 않아요.") },
            confirmButton = { ChaekOverlayButton("작성 취소", onDismiss) },
            dismissButton = {
                ChaekOverlayButton(
                    "계속 작성",
                    {
                        showDiscardConfirmation = false
                        coroutineScope.launch { sheetState.show() }
                    },
                    secondary = true,
                )
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReplyInputSheet(
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var content by rememberSaveable(initialContent) { mutableStateOf(initialContent) }
    val canSubmit = ReplyInputRules.canSubmit(content)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ChaekSurface,
        shape = ChaekOverlayTokens.sheetShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(if (initialContent.isEmpty()) "답글 작성" else "답글 수정", onDismiss)
            FormLabel("답글")
            ChaekTextInput(content, { content = it.take(ReplyInputRules.MAX_LENGTH) }, "답글을 입력하세요", "답글",
                Modifier.fillMaxWidth(), 120)
            Text("${content.length} / ${ReplyInputRules.MAX_LENGTH}", Modifier.fillMaxWidth(), color = ChaekInkSecondary,
                textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
            SheetPrimaryButton(if (initialContent.isEmpty()) "등록" else "수정 저장", canSubmit) {
                content.trim().takeIf(ReplyInputRules::canSubmit)?.let(onSave)
            }
        }
    }
}

@Composable
internal fun OwnedContentActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    @OptIn(ExperimentalMaterial3Api::class)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ChaekSurface, shape = ChaekOverlayTokens.sheetShape) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SheetHeader(title, onDismiss)
            ChaekOverlayButton("수정", onEdit, secondary = true)
            ChaekOverlayButton("삭제", onDelete, secondary = true)
        }
    }
}

@Composable
internal fun DeleteContentConfirmation(
    contentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ChaekTwoActionDialog(
        onDismissRequest = onDismiss,
        title = { Text("${contentName}을 삭제할까요?") },
        text = { Text("삭제한 ${contentName}은 다시 복구할 수 없어요.") },
        confirmButton = { ChaekOverlayButton("삭제", onConfirm) },
        dismissButton = { ChaekOverlayButton("취소", onDismiss, secondary = true) },
    )
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), color = ChaekInk, style = MaterialTheme.typography.titleMedium)
        ChaekCloseButton(onDismiss)
    }
}

@Composable
private fun FormLabel(label: String, required: Boolean = false) {
    Text(
        buildAnnotatedString {
            append(label)
            if (required) {
                append(" (")
                withStyle(SpanStyle(color = ChaekDanger)) { append("필수") }
                append(")")
            }
        },
        color = ChaekInk,
        style = if (required) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ChaekTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accessibilityLabel: String,
    modifier: Modifier,
    height: Int,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    suffix: String? = null,
    endText: String? = null,
    emphasized: Boolean = false,
) {
    val shape = ChaekOverlayTokens.inputShape
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height.dp).background(ChaekSurfaceMuted, shape)
            .then(if (emphasized) Modifier.border(1.dp, ChaekInk, shape) else Modifier)
            .semantics { contentDescription = accessibilityLabel }
            .padding(horizontal = 12.dp, vertical = if (singleLine || suffix != null || endText != null) 0.dp else 10.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ChaekInk),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine || suffix != null || endText != null,
        decorationBox = { innerTextField ->
            if (singleLine || suffix != null || endText != null) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 12.sp)
                        innerTextField()
                    }
                    suffix?.let { Text(it, color = ChaekInk, fontSize = 11.sp) }
                    endText?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(start = 6.dp),
                            color = ChaekInkSecondary,
                            fontFamily = MaterialTheme.typography.bodySmall.fontFamily,
                            fontSize = 11.sp,
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 12.sp)
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SheetPrimaryButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    ChaekOverlayButton(label, onClick, enabled = enabled)
}
