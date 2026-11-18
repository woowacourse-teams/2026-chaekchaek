package com.chaekchaek.incident.evidence.dto;

import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.evidence.QueryName;
import java.util.HashSet;
import java.util.List;

public record QuerySelection(
        List<QueryName> queries
) {
    public QuerySelection {
        if (queries == null || queries.isEmpty() || queries.size() > 4) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        var unique = new HashSet<>(queries);
        if (unique.contains(null) || unique.size() != queries.size()) {
            throw new RemoteCallException("INVALID_TOOL_ARGUMENTS");
        }
        queries = List.copyOf(queries);
    }
}
