package com.chaekchaek.incident.alert.service;

import com.chaekchaek.incident.alert.domain.Incident;
import com.chaekchaek.incident.alert.domain.IncidentReference;
import com.chaekchaek.incident.alert.dto.GrafanaAlertRequest;
import com.chaekchaek.incident.alert.dto.IncidentSnapshot;
import com.chaekchaek.incident.alert.repository.IncidentRepository;
import com.chaekchaek.incident.config.IncidentProperties;
import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidents;
    private final IncidentProperties properties;
    private final Clock clock;

    @Transactional
    public void accept(GrafanaAlertRequest request) {
        if (request.orgId() != properties.orgId()) {
            throw new IllegalArgumentException("허용된 Grafana 조직이어야 합니다");
        }
        for (var alert : request.alerts()) {
            acceptAlert(request.orgId(), alert);
        }
    }

    private void acceptAlert(long orgId, GrafanaAlertRequest.Alert alert) {
        String environment = alert.labels().get("env");
        String profile = alert.labels().get("analysis_profile");
        if (environment == null || profile == null || !properties.enabledEnvironments().contains(environment)
                || !Set.of("api-errors", "availability").contains(profile)) {
            return;
        }
        var reference = new IncidentReference(
                environment,
                alert.fingerprint(),
                alert.startsAt()
        );
        var incident = incidents.lockById(reference.key(orgId)).orElseGet(() -> new Incident(
                orgId,
                reference,
                safeName(alert.labels().get("alertname")),
                profile,
                clock.instant()
        ));
        if ("resolved".equals(alert.status())) {
            incident.resolve();
        }
        incidents.save(incident);
    }

    private String safeName(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,100}")) {
            return "grafana-alert";
        }
        return value;
    }

    @Transactional(readOnly = true)
    public Optional<IncidentSnapshot> find(IncidentReference reference) {
        if (!properties.enabledEnvironments().contains(reference.environment())) {
            return Optional.empty();
        }
        return incidents.findById(reference.key(properties.orgId()))
                .map(incident -> new IncidentSnapshot(
                        incident.getId(),
                        incident.getEnvironment(),
                        incident.getAlertName(),
                        incident.getProfile(),
                        incident.getStartsAt(),
                        incident.isResolved()
                ));
    }
}
