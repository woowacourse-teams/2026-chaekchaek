package com.chaekchaek.pmfsurvey.controller;

import com.chaekchaek.pmfsurvey.dto.PmfSurveySummaryResponse;
import com.chaekchaek.pmfsurvey.service.PmfSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/pmf-survey")
@RequiredArgsConstructor
public class AdminPmfSurveyController {

    private final PmfSurveyService pmfSurveyService;

    @GetMapping("/responses")
    public ResponseEntity<PmfSurveySummaryResponse> getResponses() {
        return ResponseEntity.ok(pmfSurveyService.getSummary());
    }
}
