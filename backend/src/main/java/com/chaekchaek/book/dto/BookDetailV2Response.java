package com.chaekchaek.book.dto;

import java.math.BigDecimal;
import java.util.List;

public record BookDetailV2Response(
        Long bookId,
        String isbn13,
        String title,
        String coverImageUrl,
        String description,
        List<String> authors,
        List<String> translators,
        String publisher,
        String category,
        String publishedDate,
        Integer totalPages,
        Integer reviewCount,
        Integer replyCount,
        BigDecimal averageRating,
        Integer ratingCount,
        Integer myRatingCount,
        BookMyRecordV2Response myRecord,
        Boolean isRegisteredInMyLibrary,
        String purchaseUrl,
        String source,
        BookQuestionResponse bookQuestion
) {

    public static BookDetailV2Response from(BookDetailResponse detail) {
        BookMyRecordV2Response myRecord = null;
        if (detail.myRecord() != null && detail.myRecord().myRating() != null) {
            myRecord = new BookMyRecordV2Response(detail.myRecord().myRating());
        }
        return new BookDetailV2Response(
                detail.bookId(),
                detail.isbn13(),
                detail.title(),
                detail.coverImageUrl(),
                detail.description(),
                detail.authors(),
                detail.translators(),
                detail.publisher(),
                detail.category(),
                detail.publishedDate(),
                detail.totalPages(),
                detail.reviewCount(),
                detail.replyCount(),
                detail.averageRating(),
                detail.ratingCount(),
                detail.myRatingCount(),
                myRecord,
                detail.isRegisteredInMyLibrary(),
                detail.purchaseUrl(),
                detail.source(),
                detail.bookQuestion()
        );
    }
}
