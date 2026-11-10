package com.chaekchaek.pmfsurvey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PmfSurveySubmitRequest(
        @NotBlank String submissionToken,
        @NotEmpty List<@NotNull @Valid PmfSurveyAnswerRequest> answers
) {
}
