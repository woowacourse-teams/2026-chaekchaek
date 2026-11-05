package com.chaekchaek.pmfsurvey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PmfSurveyAnswerRequest(
        @NotBlank String questionCode,
        @NotEmpty List<@NotBlank String> answerCodes
) {
}
