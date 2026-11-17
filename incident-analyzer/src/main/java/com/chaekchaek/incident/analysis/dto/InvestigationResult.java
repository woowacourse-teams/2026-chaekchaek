package com.chaekchaek.incident.analysis.dto;

import com.chaekchaek.incident.evidence.dto.QueryExecution;
import java.util.List;

public record InvestigationResult(
        AnalysisResult analysis,
        List<QueryExecution> queries,
        String failureCode
) {
    public InvestigationResult {
        queries = List.copyOf(queries);
    }
}
