package com.chaekchaek.pmfsurvey.controller;

import com.chaekchaek.pmfsurvey.dto.PmfSurveyAvailabilityResponse;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmitRequest;
import com.chaekchaek.pmfsurvey.dto.PmfSurveySubmissionResponse;
import com.chaekchaek.pmfsurvey.service.PmfSurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pmf-survey")
@RequiredArgsConstructor
public class PmfSurveyController {

    private final PmfSurveyService pmfSurveyService;

    @GetMapping
    public ResponseEntity<PmfSurveyAvailabilityResponse> getSurvey() {
        return ResponseEntity.ok(pmfSurveyService.getSurvey());
    }

    @PostMapping("/responses")
    public ResponseEntity<PmfSurveySubmissionResponse> submit(
            @Valid @RequestBody PmfSurveySubmitRequest request
    ) {
        return ResponseEntity.status(201).body(pmfSurveyService.submit(request));
    }
}
