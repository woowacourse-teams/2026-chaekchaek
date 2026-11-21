package com.chaekchaek.incident.evidence.client;

import com.chaekchaek.incident.common.JsonHttpClient;
import com.chaekchaek.incident.common.RemoteCallException;
import com.chaekchaek.incident.config.IncidentProperties;
import java.math.BigInteger;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class LokiClient {
    private final IncidentProperties properties;
    private final JsonHttpClient http;

    public JsonNode logs(String environment, Instant from, Instant to) {
        String query = "{app=\"chaekchaek\",env=\"" + environment
                + "\"} | json | type=\"error\" | status >= 500 | status < 600 | __error__=\"\"";
        return queryRange(query, from, to);
    }

    public JsonNode queryRange(String query, Instant from, Instant to) {
        URI uri = UriComponentsBuilder.fromUri(properties.lokiUrl())
                .path("/loki/api/v1/query_range")
                .queryParam("query", "{query}")
                .queryParam("start", nanoseconds(from))
                .queryParam("end", nanoseconds(to))
                .queryParam("limit", 200)
                .queryParam("direction", "backward")
                .encode()
                .buildAndExpand(query)
                .toUri();
        return checked(http.get(uri));
    }

    private String nanoseconds(Instant at) {
        return BigInteger.valueOf(at.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000))
                .add(BigInteger.valueOf(at.getNano()))
                .toString();
    }

    public JsonNode errorCount(String environment, Instant at) {
        String query = "sum(count_over_time({app=\"chaekchaek\",env=\"" + environment
                + "\"} | json kind=\"type\", status=\"status\" | kind=\"access\" | status >= 500 | status < 600 | __error__=\"\" [5m]))";
        return query(query, at);
    }

    public JsonNode query(String query, Instant at) {
        URI uri = UriComponentsBuilder.fromUri(properties.lokiUrl())
                .path("/loki/api/v1/query")
                .queryParam("query", "{query}")
                .queryParam("time", at.getEpochSecond())
                .encode()
                .buildAndExpand(query)
                .toUri();
        return checked(http.get(uri));
    }

    private JsonNode checked(JsonNode response) {
        if (!"success".equals(response.path("status").asText())) {
            throw new RemoteCallException("LOKI_QUERY_FAILED");
        }
        JsonNode result = response.path("data").path("result");
        if (!result.isArray()) {
            throw new RemoteCallException("INVALID_LOKI_RESPONSE");
        }
        return result;
    }
}
