package com.chaekchaek.library.repository;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

final class LibraryBookCreationExceptionTranslator {

    private static final String PLACEMENT_UNIQUE_CONSTRAINT = "uk_library_book_library_book";

    private LibraryBookCreationExceptionTranslator() {
    }

    static RuntimeException translate(DataIntegrityViolationException exception) {
        String constraint = findConstraintName(exception);
        if (constraint == null || !constraint.toLowerCase(Locale.ROOT).contains(PLACEMENT_UNIQUE_CONSTRAINT)) {
            return exception;
        }
        BusinessException translated = new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
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
