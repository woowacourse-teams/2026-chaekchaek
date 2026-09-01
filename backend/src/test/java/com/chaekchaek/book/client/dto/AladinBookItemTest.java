package com.chaekchaek.book.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.book.domain.Isbn13;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AladinBookItemTest {

    @Test
    @DisplayName("알라딘 출간일을 날짜로 변환한다")
    void should_ReturnLocalDate_When_PubDateIsValid() {
        // given
        AladinBookItem item = aladinBook("2026-07-01");

        // when
        LocalDate publishedDate = item.publishedDate();

        // then
        assertThat(publishedDate).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("알라딘 출간일이 null이거나 비어 있다면 날짜 없음으로 변환한다")
    void should_ReturnNull_When_PubDateIsNullOrBlank(String pubDate) {
        // given
        AladinBookItem item = aladinBook(pubDate);

        // when
        LocalDate publishedDate = item.publishedDate();

        // then
        assertThat(publishedDate).isNull();
    }

    @Test
    @DisplayName("알라딘 도서의 ISBN13이 요청 ISBN13과 같으면 참을 반환한다")
    void should_ReturnTrue_When_Isbn13Matches() {
        // given
        AladinBookItem item = new AladinBookItem(
                null, null, null, null, null, "9788925568683", null, null, null
        );

        // when
        boolean matches = item.matchesIsbn13(new Isbn13("9788925568683"));

        // then
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("알라딘 도서의 ISBN13이 요청 ISBN13과 다르면 거짓을 반환한다")
    void should_ReturnFalse_When_Isbn13Differs() {
        // given
        AladinBookItem item = new AladinBookItem(
                null, null, null, null, null, "9788925568683", null, null, null
        );

        // when
        boolean matches = item.matchesIsbn13(new Isbn13("9781234567897"));

        // then
        assertThat(matches).isFalse();
    }

    private AladinBookItem aladinBook(String pubDate) {
        return new AladinBookItem(
                null,
                null,
                null,
                null,
                pubDate,
                null,
                null,
                null,
                null
        );
    }
}
