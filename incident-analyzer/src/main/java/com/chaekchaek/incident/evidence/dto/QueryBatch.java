package com.chaekchaek.incident.evidence.dto;

import java.util.List;

public record QueryBatch(
        EvidenceBundle evidence,
        List<QueryExecution> executions
) {
    public QueryBatch {
        executions = List.copyOf(executions);
    }

    public boolean hasNoData() {
        return executions
                .stream()
                .anyMatch(query -> query.status() == QueryStatus.NO_DATA);
    }
}
