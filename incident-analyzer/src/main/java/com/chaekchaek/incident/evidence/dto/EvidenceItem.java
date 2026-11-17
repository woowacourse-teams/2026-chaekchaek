package com.chaekchaek.incident.evidence.dto;

import java.util.Map;

public record EvidenceItem(
        String id,
        String source,
        String description,
        Map<String, String> values
) {
    public EvidenceItem {
        values = Map.copyOf(values);
    }
}
