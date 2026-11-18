package com.chaekchaek.incident.alert.controller;

import com.chaekchaek.incident.alert.dto.GrafanaAlertRequest;
import com.chaekchaek.incident.alert.service.IncidentService;
import com.chaekchaek.incident.config.IncidentProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequiredArgsConstructor
public class GrafanaWebhookController {
    private static final int MAX_BODY_BYTES = 262144;
    private final IncidentProperties properties;
    private final IncidentService incidents;
    private final JsonMapper mapper;
    private final Validator validator;

    @PostMapping("/internal/alerts/grafana")
    public ResponseEntity<Void> accept(HttpServletRequest request) throws IOException {
        authenticate(request.getHeader("Authorization"));
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        GrafanaAlertRequest payload = mapper.readValue(body, GrafanaAlertRequest.class);
        if (!validator.validate(payload).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        incidents.accept(payload);
        return ResponseEntity.accepted().build();
    }

    private void authenticate(String header) {
        String expected = "Bearer " + properties.webhookToken();
        if (properties.webhookToken().isBlank() || header == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
