package com.chaekchaek.pmfsurvey.dto;

public record PmfSurveyAnswerCountResponse(
        String answerCode,
        String label,
        long respondentCount
) {
}
