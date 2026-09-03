package com.chaekchaek.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Disabled
class Isbn13Test {

    @ParameterizedTest
    @ValueSource(strings = {"9788925568683", "9788966260959"})
    @DisplayName("체크섬이 맞는 숫자 13자리로 ISBN13을 생성한다")
    void should_CreateIsbn13_When_Isbn13ChecksumIsValid(String value) {
        // when & then
        assertThat(new Isbn13(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9788925568682", "978892556868", "978892556868X"})
    @DisplayName("형식 또는 체크섬이 맞지 않으면 입력 오류를 던진다")
    void should_ThrowInvalidRequestException_When_Isbn13FormatOrChecksumIsInvalid(String value) {
        // when & then
        assertThatThrownBy(() -> new Isbn13(value))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
