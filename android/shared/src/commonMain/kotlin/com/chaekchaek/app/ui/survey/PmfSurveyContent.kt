package com.chaekchaek.app.ui.survey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chaekchaek.app.presentation.survey.PmfSurveyUiState
import com.chaekchaek.app.presentation.survey.PmfSurveyViewModel
import com.chaekchaek.app.ui.common.ChaekCloseButton
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import kotlin.math.abs

private val Ink = Color(0xFF191919)
private val Accent = Color(0xFFFF8500)
private val MutedSurface = Color(0xFFF3F3F5)

/** 승인 원본: visual-contracts/prototypes/pmf-survey/index.html, a688295b. Figma는 호출 한도로 미등록. */
@Composable
fun PmfSurveyBar(viewModel: PmfSurveyViewModel) {
    val state by viewModel.uiState.collectAsState()
    if (state.barVisible) {
        SurveyTypography {
            var drag by remember { mutableFloatStateOf(0f) }
            var width by remember { mutableIntStateOf(1) }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                val compact = maxWidth < 330.dp
                Row(
                    Modifier.testTag("pmf-survey-bar").semantics { testTagsAsResourceId = true }
                        .fillMaxWidth().height(if (compact) 78.dp else 89.dp)
                        .onSizeChanged { width = it.width }
                        .graphicsLayer { translationX = drag; alpha = (1f - abs(drag) / width).coerceAtLeast(.2f) }
                        .pointerInput(viewModel, width) {
                            detectHorizontalDragGestures(
                                onDragEnd = { if (abs(drag) >= width * .22f) viewModel.dismissForDay(); drag = 0f },
                                onDragCancel = { drag = 0f },
                            ) { change, amount -> change.consume(); drag += amount }
                        }
                        .clip(RoundedCornerShape(20.dp)).background(Ink)
                        .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = viewModel::open)
                        .semantics {
                            contentDescription = "책췍 설문 참여하기"
                            customActions = listOf(CustomAccessibilityAction("하루 동안 숨기기") { viewModel.dismissForDay(); true })
                        }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!compact) Box(Modifier.size(40.dp, 56.dp).background(Color.White.copy(alpha = .05f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        ClipboardMark(Modifier.size(25.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("책췍 설문", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                        Spacer(Modifier.height(2.dp))
                        Text("여러분의 의견을 들려주세요", fontSize = if (compact) 14.sp else 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.35).sp, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("밀어서 하루 동안 숨기기", fontSize = if (compact) 9.sp else 10.sp, lineHeight = 14.sp, color = Color(0xFFBDBDBD))
                    }
                    Box(Modifier.size(40.dp).background(Accent, CircleShape), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(17.dp)) {
                            val stroke = 2.dp.toPx()
                            drawLine(Ink, Offset(size.width * .28f, size.height * .72f), Offset(size.width * .72f, size.height * .28f), stroke, StrokeCap.Round)
                            drawLine(Ink, Offset(size.width * .28f, size.height * .28f), Offset(size.width * .72f, size.height * .28f), stroke, StrokeCap.Round)
                            drawLine(Ink, Offset(size.width * .72f, size.height * .28f), Offset(size.width * .72f, size.height * .72f), stroke, StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
    if (state.dialogOpen) PmfSurveyDialog(state, viewModel::close, viewModel::select, viewModel::previous, viewModel::next)
}

@Composable
internal fun PmfSurveyDialog(state: PmfSurveyUiState, onClose: () -> Unit, onSelect: (String) -> Unit, onPrevious: () -> Unit, onNext: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !state.submitting, dismissOnClickOutside = !state.submitting)) {
        SurveyTypography {
            BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().heightIn(max = maxHeight), color = Color.White, shape = RoundedCornerShape(22.dp)) {
                    if (state.completed) SurveyCompleted(onClose) else state.question?.let { question ->
                        Column {
                            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("책췍 설문", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(16.dp))
                                Text("${state.questionIndex + 1}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(" / ${state.questions.size}", fontSize = 12.sp, color = Color(0xFF858585))
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.semantics { if (state.submitting) disabled() }) { ChaekCloseButton { if (!state.submitting) onClose() } }
                            }
                            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.questions.forEachIndexed { index, _ -> Box(Modifier.weight(1f).height(3.dp).background(if (index <= state.questionIndex) Accent else Color(0xFFE7E7E9), CircleShape)) }
                            }
                            key(question.code) {
                                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
                                    Text(question.title, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.5).sp)
                                    Text(if (question.multiple) "해당하는 항목을 모두 골라 주세요." else "하나를 골라 주세요.", modifier = Modifier.padding(top = 8.dp, bottom = 20.dp), fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFF737373))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                                        question.options.forEach { option ->
                                            val selected = option.code in state.answers[question.code].orEmpty()
                                            Row(
                                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                    .background(if (selected) Color(0xFFFFF6EB) else MutedSurface)
                                                    .border(1.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(12.dp))
                                                    .selectable(selected, interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !state.submitting, role = if (question.multiple) Role.Checkbox else Role.RadioButton) { onSelect(option.code) }
                                                    .heightIn(min = 50.dp).padding(horizontal = 14.dp, vertical = 13.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                ChoiceMark(selected, question.multiple)
                                                Text(option.label, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-.2).sp)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (state.questionIndex > 0) SurveyButton("이전", onPrevious, Modifier.width(72.dp), enabled = !state.submitting, secondary = true)
                                SurveyButton(if (state.questionIndex == state.questions.lastIndex) "제출하기" else "다음", onNext, Modifier.weight(1f), enabled = state.canContinue, loading = state.showLoading)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurveyTypography(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontFamily = ChaekIconFontFamily(), color = Ink), content = content)
}

@Composable
private fun SurveyButton(label: String, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true, secondary: Boolean = false, loading: Boolean = false) {
    Box(modifier.heightIn(min = 46.dp).clip(RoundedCornerShape(12.dp))
        .background(if (!enabled) Color(0xFFE9E9ED) else if (secondary) MutedSurface else Accent)
        .clickable(remember { MutableInteractionSource() }, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp).semantics { contentDescription = "설문 제출 중" }, color = Ink, strokeWidth = 2.dp)
        else Text(label, fontSize = 14.sp, lineHeight = 22.sp, fontWeight = if (secondary) FontWeight.Normal else FontWeight.SemiBold, color = if (enabled) Ink else Color(0xFFA6A6AD))
    }
}

@Composable
private fun ChoiceMark(selected: Boolean, multiple: Boolean) {
    val shape = if (multiple) RoundedCornerShape(5.dp) else CircleShape
    Box(Modifier.size(18.dp).background(if (selected) Accent else Color.White, shape).border(1.5.dp, if (selected) Accent else Color(0xFFC4C4C9), shape), contentAlignment = Alignment.Center) {
        if (selected) {
            if (multiple) CheckMark(Modifier.size(10.dp)) else Box(Modifier.size(6.dp).background(Ink, CircleShape))
        }
    }
}

@Composable
private fun SurveyCompleted(onClose: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, end = 12.dp), horizontalArrangement = Arrangement.End) { ChaekCloseButton(onClose) }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(52.dp).background(Color(0xFFFFF1DF), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { CheckMark(Modifier.size(26.dp)) }
            Text("의견을 들려주셔서 고마워요", Modifier.padding(top = 20.dp), fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("여러분의 답변을 바탕으로\n더 나은 책췍을 만들어 갈게요.", Modifier.padding(top = 10.dp), fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF737373))
        }
        SurveyButton("확인", onClose, Modifier.fillMaxWidth().padding(20.dp))
    }
}

@Composable
private fun CheckMark(modifier: Modifier) {
    Canvas(modifier) {
        val middle = Offset(size.width * .4f, size.height * .7f)
        drawLine(Ink, Offset(size.width * .15f, size.height * .45f), middle, 2.dp.toPx(), StrokeCap.Round)
        drawLine(Ink, middle, Offset(size.width * .85f, size.height * .2f), 2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun ClipboardMark(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = Stroke(1.5.dp.toPx())
        drawRect(Accent, Offset(size.width * .25f, size.height * .17f), Size(size.width * .5f, size.height * .7f), style = stroke)
        drawRect(Accent, Offset(size.width * .375f, size.height * .08f), Size(size.width * .25f, size.height * .17f), style = stroke)
        for (fraction in listOf(.46f, .63f)) drawLine(Accent, Offset(size.width * .375f, size.height * fraction), Offset(size.width * .625f, size.height * fraction), 1.5.dp.toPx())
    }
}
