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
        List<EvidenceItem> evidence
) {
    public AnalysisResult {
        evidenceNotes = List.copyOf(evidenceNotes);
        evidence = List.copyOf(evidence);
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
