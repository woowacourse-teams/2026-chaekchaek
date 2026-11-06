package com.chaekchaek.app.ui.survey

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.domain.survey.*
import com.chaekchaek.app.domain.feed.*
import com.chaekchaek.app.domain.book.BookId
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.survey.PmfSurveyViewModel
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.common.ErrorRecoveryEffect
import kotlin.time.Clock

/** 테스트 전용 진입점. 실제 응답을 저장하지 않으며 운영 인증·숨김 상태를 사용하지 않는다. */
@Composable
fun PmfSurveyPreviewScreen() {
    val repository = remember { PreviewSurveyRepository() }
    val survey = remember { PmfSurveyViewModel(repository, object : SurveyDismissalStorage {
        var until = 0L
        override fun hiddenUntilMillis() = until
        override fun hideUntil(epochMillis: Long) { until = epochMillis }
    }, Clock.System) }
    val home = remember { HomeViewModel(object : FeedRepository {
        override suspend fun homeFeed(accessToken: String?) = HomeFeed(listOf(FeedSection.TrendingBooks(
            listOf(TrendingBook(BookId("survey-preview"), "설문 화면 확인용 책", "", 12, 3)), 1,
        )))
    }, Clock.System) }
    LaunchedEffect(Unit) { survey.authenticate(SurveyRespondent.Guest("preview-only")) }
    ErrorRecoveryEffect(survey.errors)
    HomeScreen(homeViewModel = home, surveyViewModel = survey, myDisplayName = "")
}

private class PreviewSurveyRepository : PmfSurveyRepository {
    private var attempts = 0
    override suspend fun availability(respondent: SurveyRespondent) = SurveyAvailability(true, false, "preview-only", listOf(
        SurveyQuestion("PMF_CORE", "이 제품을 더 이상 쓸 수 없다면 어떻겠습니까?", false, listOf(SurveyOption("VERY_DISAPPOINTED", "매우 실망"), SurveyOption("SLIGHTLY_DISAPPOINTED", "약간 실망"), SurveyOption("NOT_DISAPPOINTED", "실망하지 않음"), SurveyOption("NO_LONGER_USING", "더 이상 쓰지 않음"))),
        SurveyQuestion("MOST_MISSED_FEATURE", "책췍을 더 이상 사용할 수 없다면, 가장 아쉬울 기능은 무엇입니까?", false, listOf(SurveyOption("LIBRARY_AND_SHELVES", "내 서재와 책장"), SurveyOption("OTHERS_FEED", "다른 사람들 감상 보기(피드)"), SurveyOption("REVIEWS_AND_REPLIES", "감상, 답글 작성"), SurveyOption("NO_MISSING_FEATURE", "특별히 아쉬운 기능 없음"))),
        SurveyQuestion("UNKNOWN_FEATURES", "책췍을 사용하면서 있는 줄 몰랐던 기능을 모두 골라 주세요", true, listOf(SurveyOption("OTHERS_FEED", "다른 사람들 감상 보기(피드)"), SurveyOption("ADD_BOOK_TO_LIBRARY", "책 서재에 담기"), SurveyOption("WRITE_REVIEW_OR_REPLY", "감상/답글 남기기"), SurveyOption("SEARCH_BOOKS", "책 검색하기"), SurveyOption("CHANGE_NICKNAME", "닉네임 변경하기"), SurveyOption("TOGGLE_REVIEW_ANONYMITY", "감상/답글 익명으로 설정하고 해제하기"), SurveyOption("NO_UNKNOWN_FEATURES", "몰랐던 기능 없음"))),
        SurveyQuestion("RELAUNCH_INTENT", "책췍이 다시 출시된다면 사용해 볼 의향이 있습니까?", false, listOf(SurveyOption("YES", "예"), SurveyOption("NO", "아니요"), SurveyOption("UNSURE", "잘 모르겠음")))
    ))
    override suspend fun submit(respondent: SurveyRespondent, submissionToken: String, answers: List<SurveyAnswer>) {
        attempts++
        if (attempts == 1) error("preview submission failure")
    }
}
