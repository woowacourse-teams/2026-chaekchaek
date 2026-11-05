package com.chaekchaek.pmfsurvey.dto;

import java.util.List;

public record PmfSurveyQuestionResponse(
        String questionCode,
        String question,
        boolean multipleSelection,
        List<PmfSurveyOptionResponse> options
) {
}
