package com.chaekchaek.library.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RatingComparisonBookResponseTest {

    private static final Instant ADDED_AT = Instant.parse("2026-08-01T00:00:00Z");
    private static final Instant RATING_UPDATED_AT = Instant.parse("2026-08-05T00:00:00Z");

    @Test
    @DisplayName("별점 비교 응답으로 변환하면 별점을 남긴 시각을 포함한다")
    void should_IncludeRatingUpdatedAt_When_ConvertingRatedLibraryItem() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, null, ADDED_AT);
        item.rate(new BigDecimal("4.5"), RATING_UPDATED_AT);
        Book book = Book.create(
                new Isbn13("9788936433598"),
                "채식주의자",
                "https://example.com/books/cover.jpg",
                null,
                null,
                null,
                List.of("한강"),
                List.of(),
                "출판사",
                null,
                null,
                null,
                null,
                null
        );

        // when
        RatingComparisonBookResponse response = RatingComparisonBookResponse.from(item, book);

        // then
        assertThat(response.ratingUpdatedAt()).isEqualTo(RATING_UPDATED_AT);
    }
}
