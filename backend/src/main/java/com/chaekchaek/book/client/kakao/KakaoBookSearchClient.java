package com.chaekchaek.book.client.kakao;

import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.client.kakao.dto.KakaoBookDocument;
import com.chaekchaek.book.client.kakao.dto.KakaoBookSearchResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoBookSearchClient implements BookSearchClient {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE = 50;

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoBookSearchClient(
            RestClient.Builder builder,
            KakaoProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        this.restApiKey = properties.restApiKey();
    }

    @Override
    public BookSearchResult search(String query, int page) {

        try {
            KakaoBookSearchResponse response = requestSearch(query, page);
            if (response.meta() == null) {
                throw new IllegalStateException("Kakao response is missing meta");
            }

            return toBookSearchResult(response, page);
        } catch (RestClientException | IllegalStateException | DateTimeParseException exception) {
            throw new KakaoClientException(exception);
        }
    }

    private KakaoBookSearchResponse requestSearch(String query, int page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/search/book")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .queryParam("sort", "accuracy")
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .requiredBody(KakaoBookSearchResponse.class);
    }

    private BookSearchResult toBookSearchResult(
            KakaoBookSearchResponse response,
            int page
    ) {
        var meta = response.meta();

        Integer nextPage = !meta.isEnd() && page < MAX_PAGE
                ? page + 1
                : null;

        List<BookSearchItem> items = response.documents().stream()
                .map(this::toBookSearchItem)
                .flatMap(Optional::stream)
                .toList();

        return new BookSearchResult(
                meta.totalCount(),
                nextPage,
                items
        );
    }

    private Optional<BookSearchItem> toBookSearchItem(KakaoBookDocument source) {
        Optional<String> isbn13 = extractIsbn13(source.isbn());

        if (isbn13.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new BookSearchItem(
                source.title(),
                source.thumbnail(),
                source.authors(),
                source.translators(),
                toLocalDate(source.datetime()),
                isbn13.get(),
                null,
                source.publisher()
        ));
    }

    private Optional<String> extractIsbn13(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(isbn.trim().split("\\s+"))
                .filter(value -> value.matches("97[89]\\d{10}"))
                .findFirst();
    }

    private LocalDate toLocalDate(String datetime) {
        if (datetime == null || datetime.isBlank()) {
            return null;
        }

        return OffsetDateTime.parse(datetime).toLocalDate();
    }
}
