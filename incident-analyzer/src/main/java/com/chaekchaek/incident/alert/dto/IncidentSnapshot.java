package com.chaekchaek.incident.alert.dto;

import java.time.Instant;

public record IncidentSnapshot(
        String id,
        String environment,
        String alertName,
        String profile,
        Instant startsAt,
        boolean resolved
) {
}
