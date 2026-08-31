package com.chaekchaek.book.client.dto;

import static org.assertj.core.api.Assertions.assertThat;

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
