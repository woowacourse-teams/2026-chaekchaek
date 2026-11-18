package com.chaekchaek.incident.alert.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Incident {
    @Id
    private String id;
    private String environment;
    private String alertName;
    private String profile;
    private Instant startsAt;
    private Instant receivedAt;
    private boolean resolved;

    public Incident(
            long orgId,
            IncidentReference reference,
            String alertName,
            String profile,
            Instant receivedAt
    ) {
        this.id = reference.key(orgId);
        this.environment = reference.environment();
        this.startsAt = reference.startsAt();
        this.alertName = alertName;
        this.profile = profile;
        this.receivedAt = receivedAt;
    }

    public void resolve() {
        resolved = true;
    }
}
