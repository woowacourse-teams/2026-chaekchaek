package com.chaekchaek.pmfsurvey.dto;

import java.util.List;

public record PmfSurveyQuestionSummaryResponse(
        String questionCode,
        String question,
        List<PmfSurveyAnswerCountResponse> answerCounts
) {
}
