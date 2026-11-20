package com.chaekchaek.incident.analysis.dto;

import com.chaekchaek.incident.evidence.dto.EvidenceItem;
import java.util.List;

public record AnalysisResult(
        AnalysisReport report,
        String model,
        String promptVersion,
        long inputTokens,
        long outputTokens,
        List<String> evidenceNotes,
        List<EvidenceItem> evidence,
        List<NextCheck> nextChecks
) {
    public AnalysisResult {
        evidenceNotes = List.copyOf(evidenceNotes);
        evidence = List.copyOf(evidence);
        nextChecks = List.copyOf(nextChecks);
    }

    public AnalysisResult(
            AnalysisReport report,
            String model,
            String promptVersion,
            long inputTokens,
            long outputTokens,
            List<String> evidenceNotes,
            List<EvidenceItem> evidence
    ) {
        this(
                report,
                model,
                promptVersion,
                inputTokens,
                outputTokens,
                evidenceNotes,
                evidence,
                List.of()
        );
    }

    public static AnalysisResult unavailable(String reason) {
        return new AnalysisResult(
                AnalysisReport.unavailable(reason),
                "none",
                "v2-mention",
                0,
                0,
                List.of(reason),
                List.of()
        );
    }
}
