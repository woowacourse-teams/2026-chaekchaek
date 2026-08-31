package com.chaekchaek.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.client.fixture.AladinMockServer;
import com.chaekchaek.book.client.fixture.AladinResponseFixture;
import java.io.IOException;
import java.time.LocalDate;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class AladinBookClientTest {

    private AladinMockServer aladinServer;
    private AladinBookClient client;

    @BeforeEach
    void setUp() throws IOException {
        aladinServer = new AladinMockServer();
        aladinServer.start();

        client = new AladinBookClient(
                RestClient.builder(),
                aladinServer.baseUrl(),
                aladinServer.ttbKey()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        aladinServer.close();
    }

    @Test
    @DisplayName("도서를 검색하면 알라딘 요청에 검색어와 페이지 및 페이지 크기를 담는다")
    void should_IncludeQueryPageAndPageSize_When_SearchingBooks() throws InterruptedException {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.빈_검색_결과());

        // when
        client.searchBooks("마션", 1);

        // then
        aladinServer.검색_요청을_검증한다("마션", 1);
    }

    @Test
    @DisplayName("도서를 검색하면 공급자 중립 검색 결과로 변환한다")
    void should_ReturnBookSearchResult_When_SearchingBooks() {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.마션_검색_결과());

        // when
        BookSearchResult result = client.search("마션", 1);

        // then
        assertThat(result.totalCount()).isEqualTo(6);
        assertThat(result.nextPage()).isNull();
        assertThat(result.items()).singleElement().satisfies(book ->
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(book.title()).isEqualTo("마션 (알라딘 리커버 특별판)");
                    softly.assertThat(book.coverImageUrl()).isEqualTo("https://image.aladin.co.kr/martian.jpg");
                    softly.assertThat(book.authors()).containsExactly("앤디 위어");
                    softly.assertThat(book.translators()).containsExactly("박아람");
                    softly.assertThat(book.publishedDate()).isEqualTo(LocalDate.of(2026, 7, 1));
                    softly.assertThat(book.isbn13()).isEqualTo("9788925568683");
                    softly.assertThat(book.category()).isEqualTo("국내도서>소설>과학소설");
                    softly.assertThat(book.publisher()).isEqualTo("알에이치코리아(RHK)");
                })
        );
    }

    @Test
    @DisplayName("알라딘 검색 결과에 다음 페이지가 있으면 다음 요청 페이지로 변환한다")
    void should_ReturnNextPage_When_AladinSearchResultHasNextPage() {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.다음_페이지가_있는_검색_결과());

        // when
        BookSearchResult result = client.search("마션", 2);

        // then
        assertThat(result.nextPage()).isEqualTo(3);
    }

    @Test
    @DisplayName("알라딘이 검색 결과를 반환하면 응답을 DTO로 매핑한다")
    void should_MapResponseToDto_When_AladinReturnsSearchResult() {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.마션_검색_결과());

        // when
        AladinSearchResponse response = client.searchBooks("마션", 1);

        // then
        assertThat(response).isNotNull();

        assertThat(response.totalResults()).isEqualTo(6);

        assertThat(response.items())
                .singleElement()
                .satisfies(book -> SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(book.title())
                            .isEqualTo("마션 (알라딘 리커버 특별판)");
                    softly.assertThat(book.isbn13())
                            .isEqualTo("9788925568683");
                    softly.assertThat(book.categoryName())
                            .isEqualTo("국내도서>소설>과학소설");
                    softly.assertThat(book.description())
                            .isEqualTo("책 설명");
                }));
    }

    @Test
    @DisplayName("알라딘이 오류 응답을 반환하면 예외가 발생한다")
    void should_ThrowException_When_AladinReturnsErrorResponse() {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.인증키_오류());

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isInstanceOfSatisfying(
                        AladinClientException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getMessage()).contains("code=1");
                            softly.assertThat(exception.getMessage())
                                    .contains("message=잘못된 인증키입니다.");
                        })
                );
    }

    @Test
    @DisplayName("알라딘이 본문 없는 200 응답을 반환하면 외부 API 예외로 정규화한다")
    void should_ThrowAladinClientException_When_AladinReturnsEmpty200Response() {
        // given
        aladinServer.본문_없는_200_응답한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isInstanceOfSatisfying(
                        AladinClientException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getCause())
                                    .isInstanceOf(IllegalStateException.class);
                        })
                );
    }

    @Test
    @DisplayName("알라딘이 204 응답을 반환하면 외부 API 예외로 정규화한다")
    void should_ThrowAladinClientException_When_AladinReturns204Response() {
        // given
        aladinServer.콘텐츠_없는_204_응답한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isInstanceOfSatisfying(
                        AladinClientException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getCause())
                                    .isInstanceOf(IllegalStateException.class);
                        })
                );
    }

    @Test
    @DisplayName("알라딘이 500 응답을 반환하면 외부 API 예외로 정규화한다")
    void should_ThrowAladinClientException_When_AladinReturnsServerError() {
        // given
        aladinServer.서버_오류_500_응답한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isInstanceOfSatisfying(
                        AladinClientException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getCause())
                                    .isInstanceOf(RestClientException.class);
                        })
                );
    }

    @Test
    @DisplayName("알라딘 연결이 중단되면 외부 API 예외로 정규화한다")
    void should_ThrowAladinClientException_When_AladinDisconnects() {
        // given
        aladinServer.연결을_즉시_종료한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isInstanceOfSatisfying(
                        AladinClientException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getCause())
                                    .isInstanceOf(RestClientException.class);
                        })
                );
    }
}
