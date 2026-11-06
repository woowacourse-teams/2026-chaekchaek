package com.chamsae.chaekchaek

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.ui.survey.PmfSurveyPreviewScreen
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PmfSurveyInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectionsSurviveBackCloseAndFailedSubmission() {
        compose.setContent { ChaekchaekTheme { PmfSurveyPreviewScreen() } }
        compose.onNodeWithContentDescription("책췍 설문 참여하기").performClick()
        compose.onNodeWithText("다음").assertIsNotEnabled()
        compose.onNodeWithText("매우 실망").performClick()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("내 서재와 책장").performClick()
        compose.onNodeWithText("이전").performClick()
        compose.onNodeWithText("매우 실망").assertIsSelected()
        compose.onNodeWithContentDescription("닫기").performClick()
        compose.onNodeWithContentDescription("책췍 설문 참여하기").performClick()
        compose.onNodeWithText("매우 실망").assertIsSelected()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("내 서재와 책장").assertIsSelected()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("다른 사람들 감상 보기(피드)").performClick()
        compose.onNodeWithText("책 검색하기").performClick()
        compose.onNodeWithText("몰랐던 기능 없음").performScrollTo().performClick()
        compose.onNodeWithText("책 검색하기").assertIsNotSelected()
        compose.onNodeWithText("몰랐던 기능 없음").assertIsSelected()
        compose.onNodeWithText("책 검색하기").performScrollTo().performClick()
        compose.onNodeWithText("몰랐던 기능 없음").assertIsNotSelected()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithText("예").performClick()
        compose.onNodeWithText("제출하기").performClick()
        compose.onNodeWithText("설문을 제출하지 못했어요").assertIsDisplayed()
        compose.onNodeWithText("닫기").performClick()
        compose.onNodeWithText("예").assertIsSelected()
        compose.onNodeWithText("제출하기").performClick()
        compose.onNodeWithText("의견을 들려주셔서 고마워요").assertIsDisplayed()
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithContentDescription("책췍 설문 참여하기").assertDoesNotExist()
    }

    @Test fun horizontalSwipeHidesTheBarWithoutOpeningDialog() {
        compose.setContent { ChaekchaekTheme { PmfSurveyPreviewScreen() } }
        compose.onNodeWithContentDescription("책췍 설문 참여하기").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("책췍 설문 참여하기").assertDoesNotExist()
        compose.onNodeWithText("이 제품을 더 이상 쓸 수 없다면 어떻겠습니까?").assertDoesNotExist()
    }
}
