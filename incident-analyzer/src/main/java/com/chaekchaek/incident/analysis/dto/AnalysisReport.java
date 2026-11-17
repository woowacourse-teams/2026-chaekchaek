package com.chaekchaek.incident.analysis.dto;

import java.util.List;

public record AnalysisReport(
        String summary,
        String assessment,
        List<Finding> observedFacts,
        List<Hypothesis> hypotheses,
        List<String> missingEvidence
) {
    public AnalysisReport {
        observedFacts = List.copyOf(observedFacts);
        hypotheses = List.copyOf(hypotheses);
        missingEvidence = List.copyOf(missingEvidence);
    }

    public record Finding(
            String description,
            List<String> evidenceIds
    ) {
        public Finding {
            evidenceIds = List.copyOf(evidenceIds);
        }
    }

    public record Hypothesis(
            String candidate,
            List<String> evidenceIds,
            String nextCheck,
            String expectedObservation
    ) {
        public Hypothesis {
            evidenceIds = List.copyOf(evidenceIds);
        }
    }

    public static AnalysisReport unavailable(String reason) {
        return new AnalysisReport(
                "자동 분석을 완료하지 못했습니다. 원본 알림과 Grafana를 확인하세요.",
                "INSUFFICIENT_EVIDENCE",
                List.of(),
                List.of(),
                List.of(reason)
        );
    }
}
