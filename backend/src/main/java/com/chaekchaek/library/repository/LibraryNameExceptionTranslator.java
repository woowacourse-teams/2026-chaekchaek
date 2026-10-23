package com.chaekchaek.library.repository;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

final class LibraryNameExceptionTranslator {

    private static final String NAME_UNIQUE_CONSTRAINT = "uq_libraries_member_name";
    private static final Logger log = LoggerFactory.getLogger(LibraryNameExceptionTranslator.class);

    private LibraryNameExceptionTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String constraint = findConstraintName(exception);
        if (constraint == null || !constraint.toLowerCase(Locale.ROOT).contains(NAME_UNIQUE_CONSTRAINT)) {
            return exception;
        }
        ErrorCode errorCode = ErrorCode.LIBRARY_NAME_ALREADY_EXISTS;
        log.atInfo()
                .addKeyValue("type", "error")
                .addKeyValue("errorCode", errorCode.getCode())
                .addKeyValue("constraint", constraint)
                .log("책장 이름 중복");
        BusinessException translated = new BusinessException(errorCode);
        translated.initCause(exception);
        return translated;
    }

    private static String findConstraintName(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
