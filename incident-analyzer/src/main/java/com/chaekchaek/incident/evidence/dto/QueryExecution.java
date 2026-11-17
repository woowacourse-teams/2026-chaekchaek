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
    }
}
