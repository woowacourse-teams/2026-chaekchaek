package com.chaekchaek.incident.alert.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record GrafanaAlertRequest(
        long orgId,
        @Min(0) @Max(0) int truncatedAlerts,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Alert> alerts
) {
    public record Alert(
            @Pattern(regexp = "firing|resolved") @NotNull String status,
            @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9_-]+") String fingerprint,
            @NotNull Instant startsAt,
            @NotNull @Size(max = 30) Map<String, String> labels
    ) {
    }
}
