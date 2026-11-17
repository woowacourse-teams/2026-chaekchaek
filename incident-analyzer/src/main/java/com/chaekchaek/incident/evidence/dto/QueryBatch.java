package com.chaekchaek.incident.evidence.dto;

import java.util.List;

public record QueryBatch(
        EvidenceBundle evidence,
        List<QueryExecution> executions
) {
    public QueryBatch {
        executions = List.copyOf(executions);
    }
}
