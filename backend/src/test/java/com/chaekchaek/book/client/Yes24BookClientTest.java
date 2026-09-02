package com.chaekchaek.book.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.client.fixture.Yes24MockServer;
import com.chaekchaek.book.client.fixture.Yes24ResponseFixture;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestClient;

class Yes24BookClientTest {

    private Yes24MockServer yes24Server;
    private Yes24BookClient client;

    @BeforeEach
    void setUp() throws IOException {
        yes24Server = new Yes24MockServer();
        client = new Yes24BookClient(
                RestClient.builder(),
                yes24Server.baseUrl(),
                yes24Server.apiKey()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        yes24Server.close();
    }

    @Test
    @DisplayName("YES24 검색 요청을 보내고 실제 형식 응답을 검색 결과로 변환한다")
    void should_ReturnMappedResult_When_Yes24ReturnsSearchResult() throws InterruptedException {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.헤르만_헤세_검색_결과());

        // when
        BookSearchResult result = client.search("헤르만 헤세", 1);

        // then
        yes24Server.검색_요청을_검증한다("헤르만 헤세", 1);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.nextPage()).isNull();
        assertThat(result.items()).singleElement().satisfies(book ->
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(book.title()).isEqualTo("데미안");
                    softly.assertThat(book.coverImageUrl())
                            .isEqualTo("https://image.yes24.com/goods/101375809/L");
                    softly.assertThat(book.authors()).containsExactly("헤르만 헤세");
                    softly.assertThat(book.translators()).containsExactly("전영애");
                    softly.assertThat(book.publishedDate()).isEqualTo(LocalDate.of(2000, 12, 20));
                    softly.assertThat(book.isbn13()).isEqualTo("9788937460449");
                    softly.assertThat(book.category()).isEqualTo("국내도서");
                    softly.assertThat(book.publisher()).isEqualTo("민음사");
                })
        );
    }

    @ParameterizedTest
    @MethodSource("paginationCases")
    @DisplayName("YES24 페이지 정보를 다음 페이지로 변환한다")
    void should_ReturnNextPage_When_Yes24HasMoreResults(
            int currentPage,
            int pageSize,
            int totalCount,
            Integer expectedNextPage
    ) {
        // given
        yes24Server.응답한다(
                200,
                Yes24ResponseFixture.검색_결과(currentPage, pageSize, totalCount)
        );

        // when
        BookSearchResult result = client.search("데미안", currentPage);

        // then
        assertThat(result.nextPage()).isEqualTo(expectedNextPage);
    }

    @Test
    @DisplayName("YES24가 검색 결과 없음 오류를 반환하면 빈 검색 결과로 변환한다")
    void should_ReturnEmptyResult_When_Yes24ReturnsSearchNotFoundError() {
        // given
        yes24Server.응답한다(
                404,
                Yes24ResponseFixture.오류_응답("SEARCH_001", "검색 결과가 없습니다.")
        );

        // when
        BookSearchResult result = client.search("없는책", 1);

        // then
        assertThat(result).isEqualTo(new BookSearchResult(0, null, List.of()));
    }

    @Test
    @DisplayName("YES24가 요청 제한 오류를 반환하면 대체 가능한 예외로 변환한다")
    void should_ThrowFallbackAllowedException_When_Yes24ReturnsRateLimitError() {
        // given
        yes24Server.응답한다(
                429,
                Yes24ResponseFixture.오류_응답("RATE_001", "요청이 너무 많습니다.")
        );

        // when & then
        assertFallbackAllowedException();
    }

    @Test
    @DisplayName("YES24가 인증 오류를 반환하면 대체 불가능한 예외로 변환한다")
    void should_ThrowNonFallbackException_When_Yes24ReturnsAuthenticationError() {
        // given
        yes24Server.응답한다(
                401,
                Yes24ResponseFixture.오류_응답("AUTH_002", "유효하지 않은 API Key입니다.")
        );

        // when & then
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOfSatisfying(
                        Yes24ClientException.class,
                        exception -> assertThat(exception.isFallbackAllowed()).isFalse()
                );
    }

    @Test
    @DisplayName("YES24가 성공 상태로 요청 제한 오류를 반환하면 대체 가능한 예외로 변환한다")
    void should_ThrowFallbackAllowedException_When_Yes24ReturnsRateLimitInResponseBody() {
        // given
        yes24Server.응답한다(
                200,
                Yes24ResponseFixture.오류_응답("RATE_001", "요청이 너무 많습니다.")
        );

        // when & then
        assertFallbackAllowedException();
    }

    @Test
    @DisplayName("YES24 연결이 중단되면 대체 가능한 예외로 변환한다")
    void should_ThrowFallbackAllowedException_When_Yes24Disconnects() {
        // given
        yes24Server.연결을_즉시_종료한다();

        // when & then
        assertFallbackAllowedException();
    }

    @Test
    @DisplayName("YES24 응답의 출간일 형식이 잘못되면 대체 가능한 예외로 변환한다")
    void should_ThrowFallbackAllowedException_When_PublishDateIsMalformed() {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.잘못된_출간일_검색_결과());

        // when & then
        assertFallbackAllowedException();
    }

    private void assertFallbackAllowedException() {
        assertThatThrownBy(() -> client.search("데미안", 1))
                .isInstanceOfSatisfying(
                        Yes24ClientException.class,
                        exception -> assertThat(exception.isFallbackAllowed()).isTrue()
                );
    }

    private static Stream<Arguments> paginationCases() {
        return Stream.of(
                Arguments.of(2, 10, 21, 3),
                Arguments.of(2, 10, 20, null)
        );
    }
}
