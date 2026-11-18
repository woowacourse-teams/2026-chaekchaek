package com.chaekchaek.incident.evidence.dto;

import java.time.Instant;
import java.util.List;

public record EvidenceBundle(
        Instant from,
        Instant to,
        List<EvidenceItem> items,
        List<String> missingEvidence
) {
    public EvidenceBundle {
        items = List.copyOf(items);
        missingEvidence = List.copyOf(missingEvidence);
    }

    public boolean hasEvidence() {
        return !items.isEmpty();
    }
}
