package com.chaekchaek.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.mascot_outline_b
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.ui.RemoteBookImage
import com.chaekchaek.app.ui.common.ChaekOverlayButton
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekAccentSoft
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekInk
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekSurface
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import org.jetbrains.compose.resources.painterResource

@Composable
fun FirstLibraryOnboardingScreen(
    state: FirstLibraryOnboardingState,
    onStartSearch: () -> Unit,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleBook: (BookSearchResult) -> Unit,
    onReviewSelection: () -> Unit,
    onLogin: () -> Unit,
    onRetrySave: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(ChaekSurface)) {
        val contentModifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
        if (state.step == OnboardingStep.SEARCH) {
            Column(contentModifier) {
                OnboardingHeader(state.step, onBack)
                Spacer(Modifier.height(28.dp))
                SearchContent(state, onQueryChange, onSearch, onToggleBook)
            }
        } else {
            Column(contentModifier.verticalScroll(rememberScrollState())) {
                OnboardingHeader(state.step, onBack)
                Spacer(Modifier.height(28.dp))
                when (state.step) {
                    OnboardingStep.INTRO -> IntroContent()
                    OnboardingStep.SEARCH -> Unit
                    OnboardingStep.CHOICE -> ChoiceContent(state.selectedBooks, loginError = null)
                    OnboardingStep.LOGIN_ERROR -> ChoiceContent(state.selectedBooks, loginError = state.loginError)
                    OnboardingStep.SAVING -> StatusContent(
                        "내 서재에\n책을 담고 있어요",
                        "조금만 기다려 주세요.",
                        state.selectedBooks.firstOrNull(),
                        loading = state.showSaveLoading,
                    )
                    OnboardingStep.SAVE_ERROR -> StatusContent(
                        "책을 담지 못했어요",
                        state.saveError ?: "연결 상태를 확인하고 다시 시도해 주세요.",
                        state.selectedBooks.firstOrNull(),
                        loading = false,
                    )
                }
            }
        }
        BottomActions(
            state = state,
            onPrimary = when (state.step) {
                OnboardingStep.INTRO -> onStartSearch
                OnboardingStep.SEARCH -> onReviewSelection
                OnboardingStep.CHOICE, OnboardingStep.LOGIN_ERROR -> onLogin
                OnboardingStep.SAVING -> ({})
                OnboardingStep.SAVE_ERROR -> onRetrySave
            },
            onSkip = onSkip,
        )
    }
}

@Composable
private fun OnboardingHeader(step: OnboardingStep, onBack: () -> Unit) {
    if (step == OnboardingStep.INTRO) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(Res.drawable.mascot_outline_b), null, Modifier.size(32.dp))
            Text("책췍", Modifier.padding(start = 8.dp), color = ChaekInk)
        }
        return
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).semantics { contentDescription = "이전" }.clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", modifier = Modifier.clearAndSetSemantics {}, fontFamily = ChaekIconFontFamily(), style = MaterialTheme.typography.titleLarge, color = ChaekInk)
        }
        Text("첫 책 고르기", Modifier.weight(1f), textAlign = TextAlign.Center, color = ChaekInk)
        Text(
            if (step == OnboardingStep.SEARCH) "1 / 2" else "2 / 2",
            style = MaterialTheme.typography.bodySmall,
            color = ChaekInkSecondary,
        )
    }
}

@Composable
private fun IntroContent() {
    Text("내 서재의 첫 책을\n골라볼까요?", style = MaterialTheme.typography.headlineMedium, color = ChaekInk)
    Text("읽고 있는 책 한 권으로\n나만의 서재를 시작해 보세요.", Modifier.padding(top = 10.dp), color = ChaekInkSecondary)
    Surface(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        color = ChaekSurfaceMuted,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("내 서재 미리보기", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
            Spacer(Modifier.height(36.dp))
            Box(Modifier.size(width = 58.dp, height = 88.dp).background(ChaekSurface), contentAlignment = Alignment.Center) {
                Text("첫 책", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
            }
            Spacer(Modifier.height(28.dp))
            Text("어떤 책으로 시작할까요?", color = ChaekInk)
            Text("지금 읽는 책을 찾아보세요", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
        }
    }
}

@Composable
private fun SearchContent(
    state: FirstLibraryOnboardingState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleBook: (BookSearchResult) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            if (state.results.isEmpty()) "어떤 책을 읽고 있나요?" else "첫 책을 선택해 주세요",
            style = MaterialTheme.typography.headlineMedium,
            color = ChaekInk,
        )
        Text("책 제목이나 작가로 찾아보세요.", Modifier.padding(top = 6.dp), color = ChaekInkSecondary)
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            placeholder = { Text("책 제목이나 작가를 검색해요") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                Text("검색", Modifier.clickable(onClick = onSearch).padding(10.dp), color = ChaekInk)
            },
        )
        when {
            state.searching -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (state.showSearchLoading) CircularProgressIndicator(color = ChaekAccent)
            }
            state.searchError != null -> MessageCard(state.searchError)
            state.searchAttempted && state.results.isEmpty() -> MessageCard("검색 결과가 없어요")
            state.results.isEmpty() -> MessageCard("책을 찾아주세요\n한 권 이상 선택하면 다음으로 갈 수 있어요.")
            else -> LazyColumn(Modifier.weight(1f).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.results, key = { it.isbn13.ifBlank { "${it.title}-${it.creator}" } }) { book ->
                    BookRow(book, state.selectedBooks.any { it.isbn13 == book.isbn13 }, onToggleBook)
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: String) {
    Surface(Modifier.fillMaxWidth().padding(top = 16.dp), color = ChaekSurfaceMuted, shape = RoundedCornerShape(14.dp)) {
        Text(message, Modifier.padding(16.dp), color = ChaekInkSecondary)
    }
}

@Composable
private fun BookRow(book: BookSearchResult, selected: Boolean, onToggle: (BookSearchResult) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle(book) }),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ChaekAccentSoft else ChaekSurface,
        border = BorderStroke(1.dp, if (selected) ChaekAccent else ChaekBorder),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RemoteBookImage(book.coverUrl, "${book.title} 표지", Modifier.size(width = 48.dp, height = 70.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, color = ChaekInk)
                Text(book.creator, style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
            }
            Text(if (selected) "✓" else "+", style = MaterialTheme.typography.titleMedium, color = ChaekInk)
        }
    }
}

@Composable
private fun ChoiceContent(books: List<BookSearchResult>, loginError: String?) {
    Text(if (loginError != null) "고른 책은 그대로예요" else "첫 책을 골랐어요", style = MaterialTheme.typography.headlineMedium, color = ChaekInk)
    Text(
        if (loginError != null) "다시 로그인하거나 그냥 시작할 수 있어요." else "로그인하면 선택한 책이\n내 서재에 바로 담겨요.",
        Modifier.padding(top = 6.dp),
        color = ChaekInkSecondary,
    )
    books.firstOrNull()?.let { BookPreview(it) }
    loginError?.let { MessageCard(it) }
}

@Composable
private fun BookPreview(book: BookSearchResult) {
    Surface(Modifier.fillMaxWidth().padding(top = 24.dp), color = ChaekSurfaceMuted, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("내 서재 미리보기", Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
            RemoteBookImage(book.coverUrl, "${book.title} 표지", Modifier.padding(top = 18.dp).size(width = 88.dp, height = 128.dp))
            Text(book.title, Modifier.padding(top = 14.dp), maxLines = 2, textAlign = TextAlign.Center, color = ChaekInk)
        }
    }
}

@Composable
private fun StatusContent(title: String, description: String, book: BookSearchResult?, loading: Boolean) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = ChaekInk)
    Text(description, Modifier.padding(top = 8.dp), color = ChaekInkSecondary)
    book?.let { BookPreview(it) }
    if (loading) CircularProgressIndicator(Modifier.padding(top = 32.dp), color = ChaekAccent)
}

@Composable
private fun BottomActions(
    state: FirstLibraryOnboardingState,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
) {
    val enabled = when (state.step) {
        OnboardingStep.SEARCH -> state.selectedBooks.isNotEmpty()
        OnboardingStep.SAVING -> false
        else -> true
    }
    val primaryLabel = when (state.step) {
        OnboardingStep.INTRO -> "책 찾기"
        OnboardingStep.SEARCH -> if (enabled) "선택한 책으로 다음" else "책을 한 권 이상 선택해 주세요"
        OnboardingStep.CHOICE, OnboardingStep.LOGIN_ERROR -> "로그인하고 진행"
        OnboardingStep.SAVING -> "저장 중"
        OnboardingStep.SAVE_ERROR -> "저장 다시 시도"
    }
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.step == OnboardingStep.SEARCH) {
            Text("선택한 책 ${state.selectedBooks.size}권", style = MaterialTheme.typography.bodySmall, color = ChaekInkSecondary)
        }
        ChaekOverlayButton(primaryLabel, onPrimary, enabled = enabled)
        ChaekOverlayButton("건너뛰기", onSkip, secondary = true)
    }
}
