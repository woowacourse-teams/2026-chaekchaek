package com.chaekchaek.app.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.ic_close
import chaekchaek.shared.generated.resources.ic_eye_off
import com.chaekchaek.app.data.remote.BookReview
import com.chaekchaek.app.data.remote.ReviewCreateRequest
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekDanger
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun PageInputDialog(
    initialPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initialPage.toString()) }
    val page = BookDetailInputRules.validPage(value, totalPages)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp).widthIn(max = 330.dp)
                .shadow(16.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = ChaekSurface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SheetHeader(title = "어디까지 읽으셨나요?", titleSize = 16, onDismiss = onDismiss)
                Text(
                    "지금까지 읽은 쪽수를 입력하면 독서 진행률에 반영돼요.",
                    modifier = Modifier.fillMaxWidth(),
                    color = ChaekInkSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 18.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("내가 읽은 쪽수")
                    ChaekTextInput(
                        value = value,
                        onValueChange = { value = it.filter(Char::isDigit).take(7) },
                        placeholder = "0",
                        accessibilityLabel = "내가 읽은 쪽수",
                        modifier = Modifier.fillMaxWidth(),
                        height = 44,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = "쪽",
                        endText = totalPages.takeIf { it > 0 }?.let { "/ ${it}쪽" },
                        emphasized = true,
                    )
                }
                SheetPrimaryButton(
                    label = "읽은 쪽수 저장",
                    enabled = BookDetailInputRules.canSubmitPage(page),
                ) { page?.let(onSave) }
            }
        }
    }
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
    var content by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialContent) }
    var quote by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialQuote) }
    var chapter by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialChapter) }
    var pageValue by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialPageValue) }
    var isSpoiler by rememberSaveable(initialReview?.reviewId) { mutableStateOf(initialSpoiler) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ChaekSurface,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp).width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(ChaekBorder),
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetHeader(title = if (initialReview == null) "감상 남기기" else "감상 수정", titleSize = 20, onDismiss = requestDismiss)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormLabel("느낀점", required = true)
                ChaekTextInput(
                    value = content,
                    onValueChange = { content = it.take(BookDetailInputRules.MAX_CONTENT_LENGTH) },
                    placeholder = "이 구간을 읽으며 든 생각을 남겨보세요",
                    accessibilityLabel = "느낀점",
                    modifier = Modifier.fillMaxWidth(),
                    height = 132,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().height(40.dp)
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
                            if (isSpoiler) Text("✓", color = ChaekInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "스포일러",
                            modifier = Modifier.padding(start = 8.dp),
                            color = ChaekDanger,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FormLabel("인상 깊은 문구")
                ChaekTextInput(
                    value = quote,
                    onValueChange = { quote = it.take(BookDetailInputRules.MAX_QUOTE_LENGTH) },
                    placeholder = "기억하고 싶은 문장을 옮겨 적어보세요",
                    accessibilityLabel = "인상 깊은 문구",
                    modifier = Modifier.fillMaxWidth(),
                    height = 72,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (allowReadingProgress) {
                    Column(modifier = Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FormLabel("쪽수")
                        ChaekTextInput(
                            value = pageValue,
                            onValueChange = { pageValue = it.filter(Char::isDigit).take(7) },
                            placeholder = "80",
                            accessibilityLabel = "쪽수",
                            modifier = Modifier.fillMaxWidth(),
                            height = 44,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            suffix = "쪽",
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FormLabel("목차 / 챕터")
                    ChaekTextInput(
                        value = chapter,
                        onValueChange = { chapter = it.take(BookDetailInputRules.MAX_CHAPTER_LENGTH) },
                        placeholder = "Chapter 1",
                        accessibilityLabel = "목차 또는 챕터",
                        modifier = Modifier.fillMaxWidth(),
                        height = 44,
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
                        if (anonymous) "익명" else "공개",
                        modifier = Modifier.padding(start = 7.dp),
                        color = ChaekInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (anonymous) "이름을 숨겨서 표시돼요" else "‘${nickname.ifBlank { "닉네임 없음" }}’으로 표시돼요",
                        modifier = Modifier.padding(start = 7.dp),
                        color = ChaekInkSecondary,
                        fontSize = 11.5.sp,
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
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(if (initialReview == null) "감상 작성을 그만둘까요?" else "감상 수정을 그만둘까요?") },
            text = { Text("작성한 내용은 저장되지 않아요.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("작성 취소") } },
            dismissButton = { TextButton(onClick = { showDiscardConfirmation = false }) { Text("계속 작성") } },
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
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ChaekSurface) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (initialContent.isEmpty()) "답글 작성" else "답글 수정", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = content,
                onValueChange = { content = it.take(ReplyInputRules.MAX_LENGTH) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "답글" },
                label = { Text("답글을 입력하세요") },
                minLines = 3,
            )
            Surface(
                onClick = { content.trim().takeIf(ReplyInputRules::canSubmit)?.let(onSave) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (canSubmit) ChaekInk else ChaekInkSecondary,
            ) {
                Text(
                    if (initialContent.isEmpty()) "등록" else "수정 저장",
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = ChaekSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun OwnedContentActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ChaekSurface,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                color = ChaekInk,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ContentAction("수정", ChaekInk, onEdit)
            ContentAction("삭제", ChaekDanger, onDelete)
        }
    }
}

@Composable
private fun ContentAction(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        color = Color.Transparent,
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DeleteContentConfirmation(
    contentName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${contentName}을 삭제할까요?") },
        text = { Text("삭제한 ${contentName}은 다시 복구할 수 없어요.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("삭제", color = ChaekDanger) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun SheetHeader(title: String, titleSize: Int, onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = ChaekInk, fontFamily = FontFamily.Serif, fontSize = titleSize.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.ic_close),
                contentDescription = "닫기",
                modifier = Modifier.size(20.dp),
                tint = ChaekInk,
            )
        }
    }
}

@Composable
private fun FormLabel(label: String, required: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ChaekInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (required) {
            Surface(shape = RoundedCornerShape(8.dp), color = ChaekInk) {
                Text(
                    "필수",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = ChaekSurface,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
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
    val shape = RoundedCornerShape(6.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height.dp).background(ChaekSurface, shape)
            .border(if (emphasized) 1.5.dp else 1.dp, if (emphasized) ChaekInk else ChaekBorder, shape)
            .semantics { contentDescription = accessibilityLabel }
            .padding(horizontal = 12.dp, vertical = if (singleLine || suffix != null || endText != null) 0.dp else 10.dp),
        textStyle = TextStyle(color = ChaekInk, fontFamily = FontFamily.SansSerif, fontSize = 12.5.sp),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine || suffix != null || endText != null,
        decorationBox = { innerTextField ->
            if (singleLine || suffix != null || endText != null) {
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 11.5.sp)
                        innerTextField()
                    }
                    suffix?.let { Text(it, color = ChaekInk, fontSize = 11.sp) }
                    endText?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(start = 6.dp),
                            color = ChaekInkSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (value.isEmpty()) Text(placeholder, color = ChaekInkSecondary, fontSize = 11.5.sp)
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SheetPrimaryButton(
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (!enabled) ChaekInkSecondary else if (danger) MaterialTheme.colorScheme.error else ChaekInk,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = ChaekSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
