package com.chaekchaek.book.dto;

import java.math.BigDecimal;

public record BookMyRecordResponse(
        String status,
        Integer currentPage,
        BigDecimal myRating
) {
}
