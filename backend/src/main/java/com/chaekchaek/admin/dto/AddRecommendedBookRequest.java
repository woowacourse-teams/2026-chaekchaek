package com.chaekchaek.admin.dto;

import com.chaekchaek.book.dto.validation.ValidIsbn13;

public record AddRecommendedBookRequest(
        @ValidIsbn13 String isbn13
) {
}
