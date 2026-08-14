package com.chaekchaek.book.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Isbn13Test {

    @ParameterizedTest
    @ValueSource(strings = {"9788925568683", "9788966260959"})
    @DisplayName("체크섬이 맞는 숫자 13자리는 ISBN13으로 인정한다")
    void should_ReturnTrue_When_Isbn13ChecksumIsValid(String isbn13) {
        // when & then
        assertThat(Isbn13.isValid(isbn13)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9788925568682", "978892556868", "978892556868X"})
    @DisplayName("형식 또는 체크섬이 맞지 않으면 ISBN13으로 인정하지 않는다")
    void should_ReturnFalse_When_Isbn13FormatOrChecksumIsInvalid(String isbn13) {
        // when & then
        assertThat(Isbn13.isValid(isbn13)).isFalse();
    }
}
