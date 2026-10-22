package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

class LibraryCreationExceptionTranslatorTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(LibraryCreationExceptionTranslator.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "uk_libraries_member_name",
            "PUBLIC.UK_LIBRARIES_MEMBER_NAME_INDEX_9",
            "libraries.uk_libraries_member_name"
    })
    @DisplayName("원인 체인의 서재명 제약 위반을 중복 오류로 바꾸고 식별 정보를 기록한다")
    void should_TranslateAndLogDuplicateName_When_ConstraintMatches(String constraint) {
        // given
        ConstraintViolationException violation = new ConstraintViolationException(
                "constraint violation", new SQLException("duplicate"), constraint);
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "insert failed", new IllegalStateException(violation));

        // when
        RuntimeException translated = LibraryCreationExceptionTranslator.translate(exception);

        // then
        assertThat(translated).isInstanceOfSatisfying(
                BusinessException.class,
                business -> assertThat(business.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS)
        );
        assertThat(translated.getCause()).isSameAs(exception);
        assertThat(appender.list).singleElement()
                .satisfies(event -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .contains(
                                tuple("errorCode", ErrorCode.LIBRARY_NAME_ALREADY_EXISTS.getCode()),
                                tuple("constraint", constraint)
                        ));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"uk_member_nickname", "fk_libraries_member"})
    @DisplayName("제약 이름이 없거나 다른 제약이면 원래 예외를 유지한다")
    void should_PreserveException_When_ConstraintDoesNotMatch(String constraint) {
        // given
        ConstraintViolationException violation = new ConstraintViolationException(
                "constraint violation", new SQLException("invalid data"), constraint);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("insert failed", violation);

        // when
        RuntimeException translated = LibraryCreationExceptionTranslator.translate(exception);

        // then
        assertThat(translated).isSameAs(exception);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("원인 체인에 Hibernate 제약 예외가 없으면 원래 예외를 유지한다")
    void should_PreserveException_When_ConstraintViolationIsAbsent() {
        // given
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "insert failed", new SQLException("invalid data"));

        // when
        RuntimeException translated = LibraryCreationExceptionTranslator.translate(exception);

        // then
        assertThat(translated).isSameAs(exception);
        assertThat(appender.list).isEmpty();
    }
}
