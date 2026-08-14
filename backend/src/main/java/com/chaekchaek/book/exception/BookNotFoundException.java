package com.chaekchaek.book.exception;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;

public class BookNotFoundException extends BusinessException {

    public BookNotFoundException() {
        super(ErrorCode.BOOK_NOT_FOUND);
    }
}
