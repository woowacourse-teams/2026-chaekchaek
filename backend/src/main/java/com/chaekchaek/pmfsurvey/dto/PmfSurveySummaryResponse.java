package com.chaekchaek.pmfsurvey.dto;

import java.util.List;

public record PmfSurveySummaryResponse(
        long respondentCount,
        List<PmfSurveyQuestionSummaryResponse> questions
) {
}
