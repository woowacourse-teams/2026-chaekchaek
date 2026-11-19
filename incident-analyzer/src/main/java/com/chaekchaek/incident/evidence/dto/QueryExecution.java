package com.chaekchaek.incident.evidence.dto;

import java.time.Instant;
import java.util.List;

public record QueryExecution(
        String queryId,
        String source,
        String description,
        String expression,
        Instant from,
        Instant to,
        QueryStatus status,
        boolean truncated,
        String evidenceId,
        List<String> limitations
) {
    public QueryExecution {
        limitations = List.copyOf(limitations);
        if (status == QueryStatus.SUCCESS && (evidenceId == null || !evidenceId.matches("E[1-9][0-9]*"))) {
            throw new IllegalArgumentException("SUCCESS_QUERY_REQUIRES_EVIDENCE");
        }
        if (status != QueryStatus.SUCCESS && !"".equals(evidenceId)) {
            throw new IllegalArgumentException("UNOBSERVED_QUERY_CANNOT_REFERENCE_EVIDENCE");
        }
    }
}
