package com.chaekchaek.pmfsurvey.domain;

import com.chaekchaek.pmfsurvey.dto.PmfSurveyOptionResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveyQuestionResponse;
import java.util.List;

public final class PmfSurveyQuestionCatalog {

    public static final String PMF_CORE = "PMF_CORE";
    public static final String MOST_MISSED_FEATURE = "MOST_MISSED_FEATURE";
    public static final String UNKNOWN_FEATURES = "UNKNOWN_FEATURES";
    public static final String RELAUNCH_INTENT = "RELAUNCH_INTENT";
    public static final String NO_UNKNOWN_FEATURES = "NO_UNKNOWN_FEATURES";

    private static final List<PmfSurveyQuestionResponse> QUESTIONS = List.of(
            new PmfSurveyQuestionResponse(
                    PMF_CORE,
                    "이 제품을 더 이상 쓸 수 없다면 어떻겠습니까?",
                    false,
                    List.of(
                            option("VERY_DISAPPOINTED", "매우 실망"),
                            option("SLIGHTLY_DISAPPOINTED", "약간 실망"),
                            option("NOT_DISAPPOINTED", "실망하지 않음"),
                            option("NO_LONGER_USING", "더 이상 쓰지 않음")
                    )
            ),
            new PmfSurveyQuestionResponse(
                    MOST_MISSED_FEATURE,
                    "책췍을 더 이상 사용할 수 없다면, 가장 아쉬울 기능은 무엇입니까?",
                    false,
                    List.of(
                            option("LIBRARY_AND_SHELVES", "내 서재와 책장"),
                            option("OTHERS_FEED", "다른 사람들 감상 보기(피드)"),
                            option("REVIEWS_AND_REPLIES", "감상, 답글 작성"),
                            option("NO_MISSING_FEATURE", "특별히 아쉬운 기능 없음")
                    )
            ),
            new PmfSurveyQuestionResponse(
                    UNKNOWN_FEATURES,
                    "책췍을 사용하면서 있는 줄 몰랐던 기능을 모두 골라 주세요",
                    true,
                    List.of(
                            option("OTHERS_FEED", "다른 사람들 감상 보기(피드)"),
                            option("ADD_BOOK_TO_LIBRARY", "책 서재에 담기"),
                            option("WRITE_REVIEW_OR_REPLY", "감상/답글 남기기"),
                            option("SEARCH_BOOKS", "책 검색하기"),
                            option("CHANGE_NICKNAME", "닉네임 변경하기"),
                            option("TOGGLE_REVIEW_ANONYMITY", "감상/답글 익명으로 설정하고 해제하기"),
                            option(NO_UNKNOWN_FEATURES, "몰랐던 기능 없음")
                    )
            ),
            new PmfSurveyQuestionResponse(
                    RELAUNCH_INTENT,
                    "책췍이 다시 출시된다면 사용해 볼 의향이 있습니까?",
                    false,
                    List.of(
                            option("YES", "예"),
                            option("NO", "아니요"),
                            option("UNSURE", "잘 모르겠음")
                    )
            )
    );

    private PmfSurveyQuestionCatalog() {
    }

    public static List<PmfSurveyQuestionResponse> questions() {
        return QUESTIONS;
    }

    public static PmfSurveyQuestionResponse question(String questionCode) {
        return QUESTIONS.stream()
                .filter(question -> question.questionCode().equals(questionCode))
                .findFirst()
                .orElse(null);
    }

    private static PmfSurveyOptionResponse option(String code, String label) {
        return new PmfSurveyOptionResponse(code, label);
    }
}
