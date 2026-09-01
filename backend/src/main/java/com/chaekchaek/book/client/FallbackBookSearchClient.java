package com.chaekchaek.book.client;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class FallbackBookSearchClient implements BookSearchClient {

    private static final Logger log = LoggerFactory.getLogger(FallbackBookSearchClient.class);

    private final Yes24BookClient yes24BookClient;
    private final AladinBookClient aladinBookClient;

    @Override
    public BookSearchResult search(String query, int page) {
        try {
            return yes24BookClient.search(query, page);
        } catch (Yes24ClientException exception) {
            if (!exception.isFallbackAllowed()) {
                throw exception;
            }
            log.warn("YES24 search failed; using Aladin fallback");
            return aladinBookClient.search(query, page);
        }
    }
}
