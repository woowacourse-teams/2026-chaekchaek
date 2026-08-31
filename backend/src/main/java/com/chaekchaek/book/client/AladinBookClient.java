package com.chaekchaek.book.client;

import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.exception.BookNotFoundException;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Component
public class AladinBookClient implements BookSearchClient {

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

    @Override
    public BookSearchResult search(String query, int page) {
        AladinSearchResponse response = searchBooks(query, page);
        return new BookSearchResult(
                response.totalResults(),
                response.hasNextPage() ? response.startIndex() + 1 : null,
                response.items().stream()
                        .map(this::toBookSearchItem)
                        .toList()
        );
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

    public com.chaekchaek.book.client.dto.AladinBookItem findBookByIsbn13(String isbn13) {
        URI uri = new DefaultUriBuilderFactory().builder()
                .path("/ttb/api/ItemLookUp.aspx")
                .queryParam("ttbkey", ttbKey)
                .queryParam("ItemIdType", "ISBN13")
                .queryParam("ItemId", isbn13)
                .queryParam("Cover", "Big")
                .queryParam("Output", "JS")
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
        return response.items().stream()
                .filter(item -> isbn13.equals(item.isbn13()))
                .findFirst()
                .orElseThrow(BookNotFoundException::new);
    }

    private AladinSearchResponse requestBooks(URI uri) {
        return restClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .requiredBody(AladinSearchResponse.class);
    }

    private BookSearchItem toBookSearchItem(com.chaekchaek.book.client.dto.AladinBookItem source) {
        AladinContributorParser.Contributors contributors = AladinContributorParser.parse(source.author());
        return new BookSearchItem(
                source.title(),
                source.cover(),
                contributors.authors(),
                contributors.translators(),
                source.pubDate() == null ? null : LocalDate.parse(source.pubDate()),
                source.isbn13(),
                source.categoryName(),
                source.publisher()
        );
    }
}
