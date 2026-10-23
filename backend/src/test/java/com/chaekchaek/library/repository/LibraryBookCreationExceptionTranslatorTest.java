package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class LibraryBookCreationExceptionTranslatorTest {

    @Test
    @DisplayName("책장 도서 배치의 유일 제약 위반만 중복 배치 오류로 변환한다")
    void should_TranslatePlacementUniqueViolation_When_ConstraintIsPlacementUniqueKey() {
        // given
        DataIntegrityViolationException exception = integrityViolation("uk_library_book_library_book");

        // when & then
        assertThatThrownBy(() -> {
            throw LibraryBookCreationExceptionTranslator.translate(exception);
        }).isInstanceOfSatisfying(
                BusinessException.class,
                translated -> assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS)
        );
    }

    @Test
    @DisplayName("책장 도서 배치 유일 제약 외의 무결성 오류는 변환하지 않는다")
    void should_KeepIntegrityViolation_When_ConstraintIsNotPlacementUniqueKey() {
        // given
        DataIntegrityViolationException exception = integrityViolation("fk_library_book_book");

        // when
        RuntimeException actual = LibraryBookCreationExceptionTranslator.translate(exception);

        // then
        assertThat(actual).isSameAs(exception);
    }

    private DataIntegrityViolationException integrityViolation(String constraintName) {
        return new DataIntegrityViolationException(
                "integrity violation",
                new ConstraintViolationException("integrity violation", new SQLException(), constraintName)
        );
    }
}
