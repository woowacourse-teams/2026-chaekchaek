package com.chaekchaek.book.client;

import com.chaekchaek.book.client.dto.Yes24BookItem;
import com.chaekchaek.book.client.dto.Yes24SearchData;
import com.chaekchaek.book.client.dto.Yes24SearchResponse;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Primary
public class Yes24BookClient implements BookSearchClient {

    private static final int PAGE_SIZE = 10;

    private final RestClient restClient;
    private final String apiKey;

    public Yes24BookClient(
            RestClient.Builder builder,
            @Value("${yes24.base-url}") String baseUrl,
            @Value("${yes24.api-key}") String apiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
    }

    @Override
    public BookSearchResult search(String query, int page) {
        Yes24SearchResponse response;
        try {
            response = requestSearch(query, page);
        } catch (RestClientResponseException exception) {
            return handleSearchHttpError(exception);
        } catch (RestClientException | IllegalStateException exception) {
            throw new Yes24ClientException(exception);
        }

        if (!response.success()) {
            throw new Yes24ClientException(response.errorCode());
        }

        try {
            return toBookSearchResult(response.data());
        } catch (RuntimeException exception) {
            throw new Yes24ClientException(exception);
        }
    }

    private Yes24SearchResponse requestSearch(String query, int page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/goods/itemList")
                        .queryParam("query", query)
                        .queryParam("category", "BOOK")
                        .queryParam("page", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .queryParam("detail", "N")
                        .build())
                .header("X-Api-Key", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .requiredBody(Yes24SearchResponse.class);
    }

    private BookSearchResult handleSearchHttpError(RestClientResponseException exception) {
        Yes24SearchResponse errorResponse = readErrorResponse(exception);
        if (exception.getStatusCode().value() == 404
                && errorResponse != null
                && "SEARCH_001".equals(errorResponse.errorCode())) {
            return new BookSearchResult(0, null, List.of());
        }
        throw new Yes24ClientException(exception);
    }

    private BookSearchResult toBookSearchResult(Yes24SearchData data) {
        return new BookSearchResult(
                data.totalCount(),
                data.nextPage(),
                data.items().stream()
                        .map(this::toBookSearchItem)
                        .toList()
        );
    }

    private BookSearchItem toBookSearchItem(Yes24BookItem source) {
        Contributors contributors = parseContributors(source.author());
        return new BookSearchItem(
                source.title(),
                source.cover(),
                contributors.authors(),
                contributors.translators(),
                source.publishedDate(),
                source.isbn13(),
                source.goodsSortNm(),
                source.publisher()
        );
    }

    @Override
    public BookDetailItem findBookByIsbn13(Isbn13 isbn13) {
        Yes24SearchResponse response;
        try {
            response = requestBookDetail(isbn13);
        } catch (RestClientResponseException exception) {
            throw translateBookDetailHttpError(exception);
        } catch (RestClientException | IllegalStateException exception) {
            throw new Yes24ClientException(exception);
        }

        if (!response.success()) {
            throw new Yes24ClientException(response.errorCode());
        }

        try {
            return response.data().items().stream()
                    .filter(item -> item.matchesIsbn13(isbn13))
                    .findFirst()
                    .map(this::toBookDetailItem)
                    .orElseThrow(BookNotFoundException::new);
        } catch (BookNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new Yes24ClientException(exception);
        }
    }

    private Yes24SearchResponse requestBookDetail(Isbn13 isbn13) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/goods/itemDetail")
                        .queryParam("searchType", "ISBN13")
                        .queryParam("query", isbn13.value())
                        .queryParam("detail", "Y")
                        .build())
                .header("X-Api-Key", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .requiredBody(Yes24SearchResponse.class);
    }

    private RuntimeException translateBookDetailHttpError(RestClientResponseException exception) {
        Yes24SearchResponse errorResponse = readErrorResponse(exception);
        if (exception.getStatusCode().value() == 404
                && errorResponse != null
                && isBookNotFoundError(errorResponse.errorCode())) {
            return new BookNotFoundException();
        }
        return new Yes24ClientException(exception);
    }

    private boolean isBookNotFoundError(String errorCode) {
        return "GOODS_001".equals(errorCode) || "GOODS_002".equals(errorCode);
    }

    private BookDetailItem toBookDetailItem(Yes24BookItem source) {
        Contributors contributors = parseContributors(source.author());
        return new BookDetailItem(
                source.title(),
                source.cover(),
                source.description(),
                contributors.authors(),
                contributors.translators(),
                source.publishedDate(),
                source.isbn13(),
                source.goodsSortNm(),
                source.publisher(),
                source.pages()
        );
    }

    private Yes24SearchResponse readErrorResponse(RestClientResponseException exception) {
        try {
            return exception.getResponseBodyAs(Yes24SearchResponse.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Contributors parseContributors(String source) {
        if (source == null || source.isBlank()) {
            return new Contributors(List.of(), List.of());
        }

        List<String> authors = new ArrayList<>();
        List<String> translators = new ArrayList<>();
        for (String group : source.split("/")) {
            String contributor = group.trim();
            if (contributor.endsWith(" 저")) {
                authors.add(contributor.substring(0, contributor.length() - 2).trim());
            }
            if (contributor.endsWith(" 역")) {
                translators.add(contributor.substring(0, contributor.length() - 2).trim());
            }
        }
        if (authors.isEmpty() && translators.isEmpty()) {
            authors.add(source.trim());
        }
        return new Contributors(authors, translators);
    }

    private record Contributors(
            List<String> authors,
            List<String> translators
    ) {
    }
}
