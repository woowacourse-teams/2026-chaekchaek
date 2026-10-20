package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.rating_star_filled
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.common.ChaekSnackbarHost
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekDanger
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun PageInputDialog(
    initialPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val pageState = rememberLimitedTextFieldState(BookDetailInputRules.PagePolicy, initialPage.toString())
    val value = pageState.text.toString()
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
                        state = pageState,
                        policy = BookDetailInputRules.PagePolicy,
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
    anonymous: Boolean,
    nickname: String,
    initialReview: BookReview? = null,
    isSaving: Boolean = false,
    showLoading: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    onPreviewSave: ((BookDetailReviewDraft) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (ReviewCreateRequest) -> Unit,
) {
    val initialContent = initialReview?.content.orEmpty()
    val initialQuote = initialReview?.quote.orEmpty()
    val initialSpoiler = initialReview?.isSpoiler == true
    val displayedAnonymously = initialReview?.anonymous ?: anonymous
    val displayedNickname = initialReview?.authorName ?: nickname
    val contentState = rememberLimitedTextFieldState(BookDetailInputRules.ContentPolicy, initialContent, initialReview?.reviewId)
    val content = contentState.text.toString()
    val quoteState = rememberLimitedTextFieldState(BookDetailInputRules.QuotePolicy, initialQuote, initialReview?.reviewId)
    val quote = quoteState.text.toString()
    var isSpoiler by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialSpoiler) }
    // 별점 API 계약 전에는 검증용 호스트에만 노출하며 실제 감상 저장 요청과 섞지 않는다.
    var previewRating by rememberSaveable(initialReview?.reviewId) { mutableStateOf<Float?>(0f) }
    var showDiscardConfirmation by rememberSaveable(initialReview?.reviewId) { mutableStateOf(false) }
    val saving by rememberUpdatedState(isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value -> value != SheetValue.Hidden || !saving },
    )
    val coroutineScope = rememberCoroutineScope()
    val canSubmit = BookDetailInputRules.canSubmitReview(content, "", 0)
    val hasDraft = content != initialContent || quote != initialQuote || isSpoiler != initialSpoiler ||
        (onPreviewSave != null && previewRating != 0f)
    val requestDismiss = {
        if (!isSaving) {
            if (hasDraft) showDiscardConfirmation = true else onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = ChaekSurface,
        shape = ChaekOverlayTokens.sheetShape,
        dragHandle = null,
    ) {
        Box {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (initialReview == null) "감상 남기기" else "감상 수정",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 21.sp, lineHeight = 31.5.sp),
                        color = ChaekInk,
                    )
                    ChaekCloseButton(onClick = requestDismiss)
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FormLabel("감상", required = true)
                    ChaekTextInput(
                        state = contentState,
                        policy = BookDetailInputRules.ContentPolicy,
                        placeholder = "이 구간을 읽으며 든 생각을 남겨보세요",
                        accessibilityLabel = "감상 (필수)",
                        readOnly = isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        height = 120,
                        emphasized = true,
                        reviewStyle = true,
                    )
                    Text("${content.length} / 1,000", modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 18.sp),
                        color = ChaekInkSecondary)
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                            .toggleable(
                                value = isSpoiler,
                                enabled = !isSaving,
                                role = Role.Checkbox,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { isSpoiler = it },
                        shape = ChaekOverlayTokens.inputShape,
                        color = ChaekSurfaceMuted,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (isSpoiler) "●" else "○",
                                modifier = Modifier.width(24.dp).clearAndSetSemantics {},
                                color = if (isSpoiler) ChaekAccent else ChaekInkSecondary,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = ChaekIconFontFamily(),
                            )
                            Text(
                                "스포일러 (선택)",
                                color = ChaekInk,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("인상 깊은 문구 (선택)", color = ChaekInk,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 18.sp))
                    ChaekTextInput(
                        state = quoteState,
                        policy = BookDetailInputRules.QuotePolicy,
                        placeholder = "기억하고 싶은 문장",
                        accessibilityLabel = "인상 깊은 문구",
                        readOnly = isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        height = 54,
                        reviewStyle = true,
                    )
                }
                Text(
                    if (displayedAnonymously) "익명으로 ‘${displayedNickname.ifBlank { "익명" }}’가 표시돼요"
                    else "‘${displayedNickname.ifBlank { "닉네임 없음" }}’으로 표시돼요",
                    color = ChaekInkSecondary,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 18.sp),
                )
                if (onPreviewSave != null) {
                    ReviewPreviewRating(previewRating, enabled = !isSaving, onChange = { previewRating = it })
                }
                SheetPrimaryButton(label = if (initialReview == null) "감상 남기기" else "수정 저장", enabled = canSubmit && !isSaving) {
                    if (!canSubmit || isSaving) return@SheetPrimaryButton
                    val draft = BookDetailReviewDraft(
                        content = content.trim(),
                        quote = quote.trim().ifEmpty { null },
                        isSpoiler = isSpoiler,
                        rating = if (onPreviewSave != null) previewRating else null,
                    )
                    if (onPreviewSave != null) onPreviewSave(draft)
                    else onSave(buildReviewRequest(draft, initialReview))
                }
            }
            if (showLoading) RequestLoadingOverlay(Modifier.matchParentSize())
            snackbarHostState?.let { ChaekSnackbarHost(it) }
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

/** 실제 저장 계약이 없는 별점은 시안용 초안 경계 안에서만 조절한다. */
@Composable
private fun ReviewPreviewRating(rating: Float?, enabled: Boolean, onChange: (Float) -> Unit) {
    val currentOnChange by rememberUpdatedState(onChange)
    val value = rating ?: 0f
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("별점 (선택)", modifier = Modifier.weight(1f, fill = false), color = ChaekInk,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 18.sp))
        Row(
            modifier = Modifier.width(176.dp).height(44.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "별점 선택"
                    stateDescription = if (rating == null) "미선택" else "${value}점"
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..5f, 9)
                    if (!enabled) disabled()
                    setProgress { requested ->
                        if (!enabled || !requested.isFinite()) false
                        else {
                            currentOnChange((requested.coerceIn(0f, 5f) * 2f).roundToInt() / 2f)
                            true
                        }
                    }
                }
                .pointerInput(enabled) {
                    if (enabled) detectTapGestures { offset ->
                        currentOnChange((ceil(offset.x.coerceIn(0f, size.width.toFloat()) / size.width * 10f) / 2f).coerceIn(0f, 5f))
                    }
                }
                .pointerInput(enabled) {
                    if (enabled) detectDragGestures(
                        onDragStart = { offset ->
                            currentOnChange((ceil(offset.x.coerceIn(0f, size.width.toFloat()) / size.width * 10f) / 2f).coerceIn(0f, 5f))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentOnChange((ceil(change.position.x.coerceIn(0f, size.width.toFloat()) / size.width * 10f) / 2f).coerceIn(0f, 5f))
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { index ->
                Box(Modifier.width(32.dp).height(44.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(Res.drawable.rating_star_filled), contentDescription = null,
                        modifier = Modifier.size(28.dp), tint = ChaekBorderSoft)
                    val fraction = (value - index).coerceIn(0f, 1f)
                    if (fraction > 0f) Icon(
                        painterResource(Res.drawable.rating_star_filled), contentDescription = null,
                        modifier = Modifier.size(28.dp).drawWithContent {
                            clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                        }, tint = ChaekAccent,
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ReplyInputSheet(
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val contentState = rememberLimitedTextFieldState(ReplyInputRules.Policy, initialContent, initialContent)
    val content = contentState.text.toString()
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
            ChaekTextInput(contentState, ReplyInputRules.Policy, "답글을 입력하세요", "답글",
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
    state: TextFieldState,
    policy: TextInputPolicy,
    placeholder: String,
    accessibilityLabel: String,
    modifier: Modifier,
    height: Int,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    suffix: String? = null,
    endText: String? = null,
    emphasized: Boolean = false,
    readOnly: Boolean = false,
    reviewStyle: Boolean = false,
) {
    val shape = ChaekOverlayTokens.inputShape
    val value = state.text.toString()
    BasicTextField(
        state = state,
        readOnly = readOnly,
        inputTransformation = policy.inputTransformation,
        modifier = modifier.height(height.dp).background(ChaekSurfaceMuted, shape)
            .then(if (emphasized) Modifier.border(1.dp, ChaekInk, shape) else Modifier)
            .semantics { contentDescription = accessibilityLabel }
            .padding(horizontal = if (reviewStyle) 16.dp else 12.dp, vertical = if (singleLine || suffix != null || endText != null) 0.dp else if (reviewStyle) 12.dp else 10.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = ChaekInk, lineHeight = if (reviewStyle) 24.sp else MaterialTheme.typography.bodyMedium.lineHeight),
        keyboardOptions = keyboardOptions,
        lineLimits = if (singleLine || suffix != null || endText != null) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
        decorator = { innerTextField ->
            if (singleLine || suffix != null || endText != null) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = if (reviewStyle) 16.sp else 12.sp,
                            lineHeight = if (reviewStyle) 24.sp else androidx.compose.ui.unit.TextUnit.Unspecified)
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
                    if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = if (reviewStyle) 16.sp else 12.sp,
                            lineHeight = if (reviewStyle) 24.sp else androidx.compose.ui.unit.TextUnit.Unspecified)
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
