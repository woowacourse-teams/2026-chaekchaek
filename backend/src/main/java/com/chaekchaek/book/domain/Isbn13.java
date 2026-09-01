package com.chaekchaek.book.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public record Isbn13(String value) {

    private static final int ISBN13_LENGTH = 13;

    public Isbn13 {
        if (!isValid(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static boolean isValid(String value) {
        if (value == null || !value.matches("\\d{" + ISBN13_LENGTH + "}")) {
            return false;
        }

        int sum = 0;
        for (int index = 0; index < ISBN13_LENGTH; index++) {
            int digit = value.charAt(index) - '0';
            sum += index % 2 == 0 ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }
}
