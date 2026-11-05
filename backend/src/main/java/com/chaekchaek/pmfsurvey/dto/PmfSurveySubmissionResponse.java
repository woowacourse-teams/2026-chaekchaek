package com.chaekchaek.pmfsurvey.dto;

import java.time.Instant;

public record PmfSurveySubmissionResponse(
        Instant submittedAt
) {
}
