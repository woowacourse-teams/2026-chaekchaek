package com.chaekchaek.book.client.yes24;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.client.BookDetailItem;
import com.chaekchaek.book.client.fixture.Yes24MockServer;
import com.chaekchaek.book.client.fixture.Yes24ResponseFixture;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class Yes24BookClientTest {

    private static final Isbn13 ISBN13 = new Isbn13("9788937460449");

    private Yes24MockServer yes24Server;
    private Yes24BookClient client;

    @BeforeEach
    void setUp() throws IOException {
        yes24Server = new Yes24MockServer();
        client = new Yes24BookClient(
                RestClient.builder(),
                new Yes24Properties(yes24Server.baseUrl(), yes24Server.apiKey())
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        yes24Server.close();
    }

    @Test
    @DisplayName("ISBN13으로 YES24 상세 정보를 조회해 공급자 중립 결과로 변환한다")
    void should_ReturnBookDetailItem_When_FindingBookByIsbn13() throws InterruptedException {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.데미안_상세_결과());

        // when
        BookDetailItem result = client.findBookByIsbn13(ISBN13);

        // then
        yes24Server.상세_요청을_검증한다(ISBN13.value());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.title()).isEqualTo("데미안");
            softly.assertThat(result.coverImageUrl())
                    .isEqualTo("https://image.yes24.com/goods/101375809/L");
            softly.assertThat(result.spineImageUrl())
                    .isEqualTo("https://image.yes24.com/goods/101375809/SIDE/L");
            softly.assertThat(result.backImageUrl())
                    .isEqualTo("https://image.yes24.com/goods/101375809/BACK/L");
            softly.assertThat(result.description()).isEqualTo("내면의 길을 찾아가는 성장 소설");
            softly.assertThat(result.authors()).containsExactly("헤르만 헤세");
            softly.assertThat(result.translators()).containsExactly("전영애");
            softly.assertThat(result.publishedDate()).isEqualTo(LocalDate.of(2000, 12, 20));
            softly.assertThat(result.isbn13()).isEqualTo(ISBN13.value());
            softly.assertThat(result.category()).isEqualTo("국내도서");
            softly.assertThat(result.publisher()).isEqualTo("민음사");
            softly.assertThat(result.totalPages()).isEqualTo(240);
        });
    }

    @Test
    @DisplayName("YES24 응답에 책등·뒷면 이미지가 없으면 null로 변환한다")
    void should_ReturnNullImageUrls_When_Yes24DoesNotProvideBookImages() throws InterruptedException {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.데미안_상세_결과_책등_뒷면_없음());

        // when
        BookDetailItem result = client.findBookByIsbn13(ISBN13);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.spineImageUrl()).isNull();
            softly.assertThat(result.backImageUrl()).isNull();
        });
    }

    @Test
    @DisplayName("YES24에 ISBN13 상세 정보가 없으면 도서 없음 예외가 발생한다")
    void should_ThrowBookNotFoundException_When_Yes24ReturnsDetailNotFoundError() {
        // given
        yes24Server.응답한다(
                404,
                Yes24ResponseFixture.오류_응답("GOODS_002", "ISBN13에 해당하는 상품을 찾을 수 없습니다.")
        );

        // when & then
        assertThatThrownBy(() -> client.findBookByIsbn13(ISBN13))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("YES24가 요청 제한 오류를 반환하면 외부 API 예외로 변환한다")
    void should_ThrowYes24ClientException_When_Yes24ReturnsRateLimitError() {
        // given
        yes24Server.응답한다(
                429,
                Yes24ResponseFixture.오류_응답("RATE_001", "요청이 너무 많습니다.")
        );

        // when & then
        assertYes24ClientException();
    }

    @Test
    @DisplayName("YES24가 인증 오류를 반환하면 외부 API 예외로 변환한다")
    void should_ThrowYes24ClientException_When_Yes24ReturnsAuthenticationError() {
        // given
        yes24Server.응답한다(
                401,
                Yes24ResponseFixture.오류_응답("AUTH_002", "유효하지 않은 API Key입니다.")
        );

        // when & then
        assertYes24ClientException();
    }

    @Test
    @DisplayName("YES24가 성공 상태로 오류 본문을 반환하면 외부 API 예외로 변환한다")
    void should_ThrowYes24ClientException_When_Yes24ReturnsErrorBodyWithSuccessStatus() {
        // given
        yes24Server.응답한다(
                200,
                Yes24ResponseFixture.오류_응답("RATE_001", "요청이 너무 많습니다.")
        );

        // when & then
        assertYes24ClientException();
    }

    @Test
    @DisplayName("YES24 연결이 중단되면 외부 API 예외로 변환한다")
    void should_ThrowYes24ClientException_When_Yes24Disconnects() {
        // given
        yes24Server.연결을_즉시_종료한다();

        // when & then
        assertYes24ClientException();
    }

    @Test
    @DisplayName("YES24 응답의 출간일 형식이 잘못되면 외부 API 예외로 변환한다")
    void should_ThrowYes24ClientException_When_PublishDateIsMalformed() {
        // given
        yes24Server.응답한다(200, Yes24ResponseFixture.데미안_상세_결과().replace("2000-12-20", "not-a-date"));

        // when & then
        assertYes24ClientException();
    }

    private void assertYes24ClientException() {
        assertThatThrownBy(() -> client.findBookByIsbn13(ISBN13))
                .isInstanceOf(Yes24ClientException.class);
    }
}
