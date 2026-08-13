package com.chaekchaek.book.client;

import com.chaekchaek.book.client.dto.AladinSearchResponse;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Component
public class AladinBookClient {

    private static final int PAGE_SIZE = 10;

    private final RestClient restClient;
    private final String ttbKey;

    public AladinBookClient(
            RestClient.Builder builder,
            @Value("${aladin.base-url}") String baseUrl,
            @Value("${aladin.ttb-key}") String ttbKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.ttbKey = ttbKey;
    }

    public AladinSearchResponse searchBooks(String query, int page) {
        URI uri = new DefaultUriBuilderFactory().builder()
                .path("/ttb/api/ItemSearch.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("Query", query)
                .queryParam("Start", page)
                .queryParam("MaxResults", PAGE_SIZE)
                .queryParam("Cover", "Big")
                .queryParam("Output", "JS")
                .queryParam("InputEncoding", "utf-8")
                .queryParam("Version", "20131101")
                .build();

        AladinSearchResponse response;
        try {
            response = requestBooks(uri);
        } catch (RestClientException | IllegalStateException exception) {
            throw new AladinClientException(exception);
        }

        if (response.hasError()) {
            throw new AladinClientException(response.errorCode(), response.errorMessage());
        }

        return response;
    }

    private AladinSearchResponse requestBooks(URI uri) {
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .requiredBody(AladinSearchResponse.class);
    }
}
