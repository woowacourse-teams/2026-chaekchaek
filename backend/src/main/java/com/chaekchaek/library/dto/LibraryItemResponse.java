package com.chaekchaek.library.dto;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.library.domain.LibraryItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LibraryItemResponse(
        long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        String spineImageUrl,
        String backImageUrl,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        LocalDate publishedDate,
        long commentCount,
        BigDecimal rating,
        Instant addedAt
) {

    public static LibraryItemResponse from(LibraryItem item, Book book, long commentCount) {
        return new LibraryItemResponse(
                item.getBookId(),
                book.getIsbn13().value(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getSpineImageUrl(),
                book.getBackImageUrl(),
                book.getAuthors(),
                book.getTranslators(),
                book.getPublisher(),
                book.getCategory(),
                book.getPublishedDate(),
                commentCount,
                item.getRating(),
                item.getAddedAt()
        );
    }
}
