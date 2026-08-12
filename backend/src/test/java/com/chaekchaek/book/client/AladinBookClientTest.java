package com.chaekchaek.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.client.fixture.AladinMockServer;
import com.chaekchaek.book.client.fixture.AladinResponseFixture;
import java.io.IOException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

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
    @DisplayName("도서를 검색하면 알라딘 요청에 검색어와 페이지를 담는다")
    void should_IncludeQueryAndPage_When_SearchingBooks() throws InterruptedException {
        // given
        aladinServer.검색_응답한다(AladinResponseFixture.빈_검색_결과());

        // when
        client.searchBooks("마션", 1);

        // then
        aladinServer.검색_요청을_검증한다("마션", 1);
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
                        AladinApiException.class,
                        exception -> SoftAssertions.assertSoftly(softly -> {
                            softly.assertThat(exception.getErrorCode())
                                    .isEqualTo(1);
                            softly.assertThat(exception.getMessage())
                                    .contains("키를 입력해 주세요");
                        })
                );
    }

    @Test
    @DisplayName("알라딘이 본문 없는 200 응답을 반환하면 필수 본문 예외가 발생한다")
    void should_ThrowRequiredBodyException_When_AladinReturnsEmpty200Response() {
        // given
        aladinServer.본문_없는_200_응답한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("The body must not be null");
    }

    @Test
    @DisplayName("알라딘이 204 응답을 반환하면 필수 본문 예외가 발생한다")
    void should_ThrowRequiredBodyException_When_AladinReturns204Response() {
        // given
        aladinServer.콘텐츠_없는_204_응답한다();

        // when & then
        assertThatThrownBy(() -> client.searchBooks("마션", 1))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("The body must not be null");
    }
}
