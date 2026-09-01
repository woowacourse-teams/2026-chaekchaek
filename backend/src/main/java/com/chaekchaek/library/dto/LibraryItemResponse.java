package com.chaekchaek.library.dto;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LibraryItemResponse(
        long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        LocalDate publishedDate,
        Integer totalPages,
        long commentCount,
        ReadingStatus status,
        int currentPage,
        BigDecimal rating,
        Instant addedAt,
        Instant readingUpdatedAt
) {

    public static LibraryItemResponse from(LibraryItem item, Book book, long commentCount) {
        return new LibraryItemResponse(
                item.getBookId(), book.getIsbn13().value(), book.getTitle(), book.getCoverImageUrl(),
                book.getAuthors(), book.getTranslators(), book.getPublisher(), book.getCategory(),
                book.getPublishedDate(), book.getTotalPages(), commentCount,
                item.getStatus(), item.getCurrentPage(),
                item.getRating(), item.getAddedAt(), item.getReadingUpdatedAt()
        );
    }
}
