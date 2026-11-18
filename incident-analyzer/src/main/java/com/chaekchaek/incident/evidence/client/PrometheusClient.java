package com.chaekchaek.incident.evidence.client;

import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.config.IncidentProperties;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class PrometheusClient {
    private final IncidentProperties properties;
    private final JsonHttpClient http;

    public JsonNode query(String query, Instant at) {
        URI uri = UriComponentsBuilder.fromUri(properties.prometheusUrl())
                .path("/api/v1/query")
                .queryParam("query", "{query}")
                .queryParam("time", at.getEpochSecond())
                .queryParam("timeout", "2s")
                .encode()
                .buildAndExpand(query)
                .toUri();
        JsonNode response = http.get(uri);
        if (!"success".equals(response.path("status").asText())) {
            throw new RemoteCallException("PROMETHEUS_QUERY_FAILED");
        }
        return response.path("data").path("result");
    }
}
