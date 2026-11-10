package com.chaekchaek.pmfsurvey.dto;

import java.util.List;

public record PmfSurveyAvailabilityResponse(
        boolean eligible,
        boolean alreadySubmitted,
        String submissionToken,
        List<PmfSurveyQuestionResponse> questions
) {
}
