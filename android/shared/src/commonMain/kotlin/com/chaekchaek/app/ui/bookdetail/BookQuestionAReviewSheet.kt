package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.rating_star_filled
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.common.ChaekTwoActionDialog
import com.chaekchaek.app.ui.input.TextInputPolicy
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekBorderSoft
import com.chaekchaek.app.ui.theme.ChaekDanger
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekOverlayTokens
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/** 감상 입력 초안. 감상 본문과 개인 서재 별점의 저장은 호출자가 맡는다. */
data class QuestionAReviewDraft(
    val content: String = "",
    val quote: String = "",
    val isSpoiler: Boolean = false,
    val rating: Double? = null,
    val anonymous: Boolean? = null,
)

internal object QuestionAReviewRules {
    fun normalize(draft: QuestionAReviewDraft): QuestionAReviewDraft = draft.copy(
        content = BookDetailInputRules.ContentPolicy.limit(draft.content),
        quote = BookDetailInputRules.QuotePolicy.limit(draft.quote),
        rating = draft.rating?.takeIf { it.isFinite() && it in 0.0..5.0 },
    )

    fun canSubmit(draft: QuestionAReviewDraft): Boolean =
        draft.content.isNotBlank() && draft.content.length <= BookDetailInputRules.MAX_CONTENT_LENGTH &&
            draft.quote.length <= BookDetailInputRules.MAX_QUOTE_LENGTH

    fun hasChanges(draft: QuestionAReviewDraft, initial: QuestionAReviewDraft): Boolean =
        normalize(draft) != normalize(initial)

    fun ratingAtFraction(fraction: Float, allowZeroRating: Boolean = true): Double {
        val minimumHalfStars = if (allowZeroRating) 0 else 1
        val bounded = fraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        return (minimumHalfStars + bounded * (10 - minimumHalfStars)).roundToInt() / 2.0
    }

    fun ratingAtProgress(progress: Float, allowZeroRating: Boolean = true): Double =
        ((progress.takeIf { it.isFinite() } ?: 0f) * 2).roundToInt()
            .coerceIn(if (allowZeroRating) 0 else 1, 10) / 2.0
}

/** 승인 Figma 1400:6119 / ReviewRating 1293:2326. 저장은 호출자가 소유한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionAReviewInputSheet(
    signedIn: Boolean,
    nickname: String,
    initialDraft: QuestionAReviewDraft = QuestionAReviewDraft(),
    anonymous: Boolean = true,
    editing: Boolean = false,
    allowZeroRating: Boolean = true,
    draftKey: String? = null,
    isSaving: Boolean = false,
    showLoading: Boolean = false,
    onDismiss: () -> Unit,
    onLoginRequired: () -> Unit,
    onSave: (QuestionAReviewDraft) -> Unit,
    onAnonymousChange: (Boolean) -> Unit = {},
) {
    val initial = remember(draftKey) { QuestionAReviewRules.normalize(initialDraft) }
    val contentState = rememberLimitedTextFieldState(BookDetailInputRules.ContentPolicy, initial.content, draftKey)
    val quoteState = rememberLimitedTextFieldState(BookDetailInputRules.QuotePolicy, initial.quote, draftKey)
    var anonymousChoice by rememberSaveable(draftKey) { mutableStateOf(anonymous) }
    var spoiler by rememberSaveable(draftKey) { mutableStateOf(initial.isSpoiler) }
    var rating by rememberSaveable(draftKey) { mutableStateOf(initial.rating) }
    var ratingTouched by rememberSaveable(draftKey) { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable(draftKey) { mutableStateOf(false) }
    val initialRating = QuestionAReviewRules.normalize(initialDraft).rating
    LaunchedEffect(draftKey, initialRating) {
        if (!ratingTouched) rating = initialRating
    }
    val discardBaseline = initial.copy(rating = initialRating, anonymous = if (editing || !signedIn) null else anonymous)
    val saving by rememberUpdatedState(isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !saving },
    )
    val coroutineScope = rememberCoroutineScope()
    val resumeWriting: () -> Unit = {
        showDiscardConfirmation = false
        coroutineScope.launch { sheetState.show() }
    }
    val draft = QuestionAReviewRules.normalize(QuestionAReviewDraft(
        content = contentState.text.toString(),
        quote = quoteState.text.toString(),
        isSpoiler = spoiler,
        rating = rating,
        anonymous = if (editing || !signedIn) null else anonymousChoice,
    ))
    val requestDismiss = {
        if (!isSaving) {
            if (QuestionAReviewRules.hasChanges(draft, discardBaseline)) showDiscardConfirmation = true else onDismiss()
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
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        Box {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                    .padding(ChaekOverlayTokens.contentPadding),
                verticalArrangement = Arrangement.spacedBy(ChaekOverlayTokens.sectionSpacing),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (editing) "감상 수정" else "감상 남기기", Modifier.weight(1f), color = ChaekInk, style = MaterialTheme.typography.titleMedium)
                    ChaekCloseButton {
                        // 시트의 입력 포커스를 해제해 큰 글꼴의 계속 작성 버튼이 키보드에 가려지지 않게 한다.
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        requestDismiss()
                    }
                }
                Text(
                    buildAnnotatedString {
                        append("감상 (")
                        withStyle(SpanStyle(color = ChaekDanger)) { append("필수") }
                        append(")")
                    },
                    color = ChaekInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
                QuestionAReviewTextInput(
                    contentState, BookDetailInputRules.ContentPolicy,
                    "이 구간을 읽으며 든 생각을 남겨보세요", "감상 (필수)", 120, emphasized = true, readOnly = isSaving,
                )
                Text(
                    "${draft.content.length} / 1,000", modifier = Modifier.fillMaxWidth(),
                    color = ChaekInkSecondary, textAlign = TextAlign.End, style = MaterialTheme.typography.labelSmall,
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        .background(ChaekSurfaceMuted, ChaekOverlayTokens.inputShape)
                        .toggleable(
                            value = spoiler,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !isSaving,
                            role = Role.Checkbox,
                            onValueChange = { spoiler = it },
                        ).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(if (spoiler) "●" else "○", fontFamily = ChaekIconFontFamily(), color = ChaekInkSecondary)
                    Text("스포일러 (선택)", color = ChaekInk, style = MaterialTheme.typography.bodySmall)
                }
                Text("인상 깊은 문구 (선택)", color = ChaekInk, style = MaterialTheme.typography.labelSmall)
                QuestionAReviewTextInput(
                    quoteState, BookDetailInputRules.QuotePolicy,
                    "기억하고 싶은 문장", "인상 깊은 문구 (선택)", 54, readOnly = isSaving,
                )
                if (!editing) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 50.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, ChaekOverlayTokens.inputShape)
                            .toggleable(value = anonymousChoice, enabled = !isSaving, role = Role.Checkbox,
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                                onValueChange = { value ->
                                    if (signedIn) { anonymousChoice = value; onAnonymousChange(value) }
                                    else onLoginRequired()
                                })
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(if (anonymousChoice) "☑" else "□", fontFamily = ChaekIconFontFamily(), color = ChaekInkSecondary)
                        Text("익명으로 남기기", color = ChaekInk, style = MaterialTheme.typography.bodySmall)
                    }
                } else Text(
                    if (anonymous) "익명으로 ‘${nickname.ifBlank { "익명" }}’가 표시돼요"
                    else "‘${nickname.ifBlank { "닉네임 없음" }}’으로 표시돼요",
                    color = ChaekInkSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
                QuestionAReviewRating(
                    rating = draft.rating,
                    allowZeroRating = allowZeroRating,
                    enabled = !isSaving,
                    onSelect = { selected ->
                        if (signedIn) {
                            ratingTouched = true
                            rating = selected
                        } else onLoginRequired()
                    },
                )
                ChaekOverlayButton(if (editing) "수정 저장" else "감상 남기기", enabled = QuestionAReviewRules.canSubmit(draft) && !isSaving, onClick = {
                    if (QuestionAReviewRules.canSubmit(draft) && !isSaving) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onSave(draft.copy(content = draft.content.trim(), quote = draft.quote.trim()))
                    }
                })
            }
            if (showLoading) RequestLoadingOverlay(Modifier.matchParentSize())
        }
    }
    if (showDiscardConfirmation) {
        ChaekTwoActionDialog(
            onDismissRequest = resumeWriting,
            title = { Text(if (editing) "감상 수정을 그만둘까요?" else "감상 작성을 그만둘까요?") },
            text = { Text("작성한 내용은 저장되지 않아요.") },
            confirmButton = { ChaekOverlayButton("작성 취소", onDismiss) },
            dismissButton = { ChaekOverlayButton("계속 작성", resumeWriting, secondary = true) },
        )
    }
}

@Composable
private fun QuestionAReviewTextInput(
    state: TextFieldState,
    policy: TextInputPolicy,
    placeholder: String,
    label: String,
    height: Int,
    emphasized: Boolean = false,
    readOnly: Boolean = false,
) {
    val textStyle = MaterialTheme.typography.bodyMedium
    val minimumLineHeight = with(LocalDensity.current) { textStyle.lineHeight.toDp() } + 24.dp
    BasicTextField(
        state = state,
        readOnly = readOnly,
        inputTransformation = policy.inputTransformation,
        lineLimits = TextFieldLineLimits.MultiLine(),
        textStyle = textStyle.copy(color = ChaekInk),
        modifier = Modifier.fillMaxWidth().height(maxOf(height.dp, minimumLineHeight))
            .background(ChaekSurfaceMuted, ChaekOverlayTokens.inputShape)
            .then(if (emphasized) Modifier.border(1.dp, ChaekInk, ChaekOverlayTokens.inputShape) else Modifier)
            .semantics { contentDescription = label }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        decorator = { inner ->
            Box(Modifier.fillMaxSize()) {
                if (state.text.isEmpty()) Text(placeholder, color = ChaekInkSecondary, style = MaterialTheme.typography.bodyMedium)
                inner()
            }
        },
    )
}

@Composable
private fun QuestionAReviewRating(rating: Double?, allowZeroRating: Boolean, enabled: Boolean, onSelect: (Double) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "별점 (선택)", modifier = Modifier.weight(1f, fill = false),
            color = ChaekInk, style = MaterialTheme.typography.labelSmall,
        )
        // 하나의 176x44 조절 컨트롤로 취급해 별의 반쪽마다 좁은 접근성 타깃을 만들지 않는다.
        Row(
            modifier = Modifier.width(176.dp).height(44.dp)
                .semantics(mergeDescendants = true) {
                    if (!enabled) disabled()
                    contentDescription = "별점 (선택)"
                    stateDescription = rating?.let { "${it}점" } ?: "미선택"
                    val minimum = if (allowZeroRating) 0f else 0.5f
                    progressBarRangeInfo = ProgressBarRangeInfo((rating ?: minimum.toDouble()).toFloat(), minimum..5f, if (allowZeroRating) 9 else 8)
                    setProgress { requested ->
                        if (!enabled || !requested.isFinite()) false else {
                            onSelect(QuestionAReviewRules.ratingAtProgress(requested, allowZeroRating))
                            true
                        }
                    }
                }
                .pointerInput(onSelect, enabled, allowZeroRating) {
                    detectTapGestures { point ->
                        // 왼쪽 8dp는 프리뷰의 유효한 0점이며 다시 탭해도 null로 취소하지 않는다.
                        val zeroEdge = 8.dp.toPx()
                        val fraction = (point.x - zeroEdge) / (size.width - zeroEdge)
                        if (enabled) onSelect(QuestionAReviewRules.ratingAtFraction(
                            if (allowZeroRating && point.x > zeroEdge) fraction.coerceAtLeast(0.1f) else fraction,
                            allowZeroRating,
                        ))
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(5) { index ->
                val fill = ((rating ?: 0.0) - index).coerceIn(0.0, 1.0).toFloat()
                Box(Modifier.width(32.dp).height(44.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(Res.drawable.rating_star_filled), null, Modifier.size(28.dp), tint = ChaekBorderSoft)
                    if (fill > 0f) Icon(
                        painterResource(Res.drawable.rating_star_filled), null,
                        Modifier.size(28.dp).drawWithContent {
                            clipRect(right = size.width * fill) { this@drawWithContent.drawContent() }
                        },
                        tint = ChaekAccent,
                    )
                }
            }
        }
    }
}
