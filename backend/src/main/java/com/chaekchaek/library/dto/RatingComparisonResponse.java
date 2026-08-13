package com.chaekchaek.library.dto;

public record RatingComparisonResponse(
        RatingComparisonBookResponse lower,
        RatingComparisonBookResponse current,
        RatingComparisonBookResponse higher
) {
}
