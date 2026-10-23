package com.chaekchaek.book.client.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

public class KakaoBookSearchClientTest {

    private static final String TEST_API_KEY = "test-kakao-rest-api-key";

    private MockWebServer kakaoServer;
    private KakaoBookSearchClient client;

    @BeforeEach
    void setUp() throws IOException {
        kakaoServer = new MockWebServer();
        kakaoServer.start();
        client = new KakaoBookSearchClient(
                RestClient.builder(),
                new KakaoProperties(kakaoServer.url("/").toString(), TEST_API_KEY)
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        kakaoServer.shutdown();
    }

    @Test
    @DisplayName("카카오 검색 요청을 보내고 응답을 기존 검색 모델로 변환한다")
    void should_SearchAndMapResponse_When_KakaoReturnsBooks() throws InterruptedException {
        // given
        enqueueJson(200, """
                {
                  "meta": {
                    "total_count": 27,
                    "pageable_count": 10,
                    "is_end": false
                  },
                  "documents": [
                    {
                      "title": "미움받을 용기",
                      "thumbnail": "https://example.com/cover.jpg",
                      "authors": ["기시미 이치로", "고가 후미타케"],
                      "translators": ["전경아"],
                      "publisher": "인플루엔셜",
                      "datetime": "2014-11-17T00:00:00.000+09:00",
                      "isbn": "8996991341 9788996991342"
                    }
                  ]
                }
                """);

        // when
        BookSearchResult result = client.search("미움받을 용기", 1);

        // then
        assertThat(result).isEqualTo(new BookSearchResult(
                27,
                2,
                List.of(new BookSearchItem(
                        "미움받을 용기",
                        "https://example.com/cover.jpg",
                        List.of("기시미 이치로", "고가 후미타케"),
                        List.of("전경아"),
                        LocalDate.of(2014, 11, 17),
                        "9788996991342",
                        null,
                        "인플루엔셜"
                ))
        ));

        RecordedRequest request = takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getRequestUrl()).isNotNull();
        assertThat(request.getRequestUrl().encodedPath()).isEqualTo("/v3/search/book");
        assertThat(request.getRequestUrl().queryParameter("query")).isEqualTo("미움받을 용기");
        assertThat(request.getRequestUrl().queryParameter("page")).isEqualTo("1");
        assertThat(request.getRequestUrl().queryParameter("size")).isEqualTo("10");
        assertThat(request.getRequestUrl().queryParameter("sort")).isEqualTo("accuracy");
        assertThat(request.getRequestUrl().queryParameterNames())
                .containsExactlyInAnyOrder("query", "page", "size", "sort");
        assertThat(request.getHeader("Authorization")).isEqualTo("KakaoAK " + TEST_API_KEY);
    }

    @Test
    @DisplayName("카카오가 마지막 페이지라고 알리면 다음 페이지를 반환하지 않는다")
    void should_ReturnNullNextPage_When_KakaoMarksLastPage() {
        // given
        enqueueJson(200, emptyResultJson(15, 10, true));

        // when
        BookSearchResult result = client.search("데미안", 1);

        // then
        assertThat(result.totalCount()).isEqualTo(15);
        assertThat(result.nextPage()).isNull();
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("카카오 최대 페이지에서는 다음 페이지를 반환하지 않는다")
    void should_ReturnNullNextPage_When_CurrentPageIsKakaoMaximumPage() {
        // given
        enqueueJson(200, emptyResultJson(500, 50, false));

        // when
        BookSearchResult result = client.search("데미안", 50);

        // then
        assertThat(result.totalCount()).isEqualTo(500);
        assertThat(result.nextPage()).isNull();
    }

    @Test
    @DisplayName("ISBN13이 없거나 기준에 맞지 않는 문서는 검색 결과에서 제외한다")
    void should_ExcludeDocumentsWithoutIsbn13() {
        // given
        enqueueJson(200, """
                {
                  "meta": {"total_count": 7, "pageable_count": 7, "is_end": true},
                  "documents": [
                    {"title": "ISBN10만 있는 책", "isbn": "8996991341"},
                    {"title": "잘못된 접두사", "isbn": "1234567890123"},
                    {"title": "짧은 ISBN", "isbn": "978123456789"},
                    {"title": "긴 ISBN", "isbn": "97912345678901"},
                    {"title": "ISBN 누락"},
                    {"title": "ISBN null", "isbn": null},
                    {"title": "공백 ISBN", "isbn": "   "}
                  ]
                }
                """);

        // when
        BookSearchResult result = client.search("책", 1);

        // then
        assertThat(result.totalCount()).isEqualTo(7);
        assertThat(result.items()).isEmpty();
    }

    @Test
    @DisplayName("979로 시작하는 13자리 ISBN도 검색 결과에 포함한다")
    void should_IncludeDocument_When_Isbn13StartsWith979() {
        // given
        enqueueJson(200, """
                {
                  "meta": {"total_count": 1, "pageable_count": 1, "is_end": true},
                  "documents": [
                    {"title": "979 ISBN 책", "isbn": "9791234567896"}
                  ]
                }
                """);

        // when
        BookSearchResult result = client.search("979 ISBN 책", 1);

        // then
        assertThat(result.items())
                .singleElement()
                .extracting(BookSearchItem::isbn13)
                .isEqualTo("9791234567896");
    }

    @Test
    @DisplayName("카카오 검색 결과가 없으면 빈 결과를 반환한다")
    void should_ReturnEmptyResult_When_KakaoReturnsNoDocuments() {
        // given
        enqueueJson(200, emptyResultJson(0, 0, true));

        // when
        BookSearchResult result = client.search("검색 결과 없음", 1);

        // then
        assertThat(result).isEqualTo(new BookSearchResult(0, null, List.of()));
    }

    @Test
    @DisplayName("카카오 API가 오류 응답을 반환하면 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_KakaoReturnsHttpError() {
        // given
        enqueueJson(401, """
                {"msg": "Unauthorized", "code": -401}
                """);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    @Test
    @DisplayName("카카오 API 호출 제한 오류를 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_KakaoRateLimitIsExceeded() {
        // given
        enqueueJson(429, """
                {"msg": "Too many requests", "code": -10}
                """);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    @Test
    @DisplayName("카카오 API 서버 오류를 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_KakaoReturnsServerError() {
        // given
        enqueueJson(500, """
                {"msg": "Internal server error", "code": -500}
                """);

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    @Test
    @DisplayName("카카오 API 연결이 중단되면 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_KakaoConnectionIsDropped() {
        // given
        kakaoServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    @Test
    @DisplayName("카카오 API 응답이 타임아웃되면 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_KakaoResponseTimesOut() {
        // given
        kakaoServer.enqueue(new MockResponse()
                .setHeadersDelay(4, TimeUnit.SECONDS)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(emptyResultJson(0, 0, true)));

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    @Test
    @DisplayName("출간일 형식이 잘못되면 카카오 클라이언트 예외로 변환한다")
    void should_ThrowKakaoClientException_When_DatetimeIsMalformed() {
        // given
        enqueueJson(200, """
                {
                  "meta": {"total_count": 1, "pageable_count": 1, "is_end": true},
                  "documents": [
                    {
                      "title": "책",
                      "datetime": "not-a-date",
                      "isbn": "9788996991342"
                    }
                  ]
                }
                """);

        // when & then
        assertThatThrownBy(() -> client.search("책", 1))
                .isInstanceOf(KakaoClientException.class);
    }

    private void enqueueJson(int status, String body) {
        kakaoServer.enqueue(new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(body));
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        RecordedRequest request = kakaoServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        return request;
    }

    private String emptyResultJson(int totalCount, int pageableCount, boolean isEnd) {
        return """
                {
                  "meta": {
                    "total_count": %d,
                    "pageable_count": %d,
                    "is_end": %s
                  },
                  "documents": []
                }
                """.formatted(totalCount, pageableCount, isEnd);
    }
}
