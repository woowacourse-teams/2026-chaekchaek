package com.chaekchaek.incident.common;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonHttpClient {
    private static final int MAX_RESPONSE_BYTES = 2_000_000;
    private final JsonMapper mapper;

    public JsonHttpClient(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode get(URI uri) {
        return execute(uri, HttpMethod.GET, null, Map.of(), Duration.ofSeconds(3));
    }

    public JsonNode get(URI uri, Map<String, String> headers) {
        return execute(uri, HttpMethod.GET, null, headers, Duration.ofSeconds(3));
    }

    public JsonNode post(URI uri, Object body, Map<String, String> headers, Duration timeout) {
        return execute(uri, HttpMethod.POST, body, headers, timeout);
    }

    private JsonNode execute(
            URI uri,
            HttpMethod method,
            Object body,
            Map<String, String> headers,
            Duration timeout
    ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(timeout);
        var request = RestClient.builder().requestFactory(factory).build().method(method).uri(uri);
        headers.forEach(request::header);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.exchange((sent, response) -> {
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RemoteCallException("HTTP_" + response.getStatusCode().value());
            }
            byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
            if (bytes.length > MAX_RESPONSE_BYTES) {
                throw new RemoteCallException("RESPONSE_TOO_LARGE");
            }
            if (bytes.length == 0 || new String(bytes, StandardCharsets.UTF_8).equals("ok")) {
                return mapper.createObjectNode();
            }
            return mapper.readTree(bytes);
        });
    }
}
