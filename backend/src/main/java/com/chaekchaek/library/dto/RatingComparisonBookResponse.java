package com.chaekchaek.library.dto;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.library.domain.LibraryItem;
import java.math.BigDecimal;

public record RatingComparisonBookResponse(
        long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        java.util.List<String> authors,
        BigDecimal myRating
) {

    public static RatingComparisonBookResponse from(LibraryItem item, Book book) {
        return new RatingComparisonBookResponse(item.getBookId(), book.getIsbn13(), book.getTitle(),
                book.getCoverImageUrl(), book.getAuthors(), item.getRating());
    }
}
