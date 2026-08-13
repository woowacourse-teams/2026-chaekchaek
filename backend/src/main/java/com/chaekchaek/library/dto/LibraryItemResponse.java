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
        ReadingStatus status,
        int currentPage,
        Integer totalPages,
        BigDecimal rating,
        Instant addedAt,
        Instant readingUpdatedAt
) {

    public static LibraryItemResponse from(LibraryItem item, Book book) {
        return new LibraryItemResponse(
                item.getBookId(), book.getIsbn13(), book.getTitle(), book.getCoverImageUrl(),
                book.getAuthors(), book.getTranslators(), book.getPublisher(), book.getCategory(),
                book.getPublishedDate(), item.getStatus(), item.getCurrentPage(), item.getTotalPages(),
                item.getRating(), item.getAddedAt(), item.getReadingUpdatedAt()
        );
    }
}
