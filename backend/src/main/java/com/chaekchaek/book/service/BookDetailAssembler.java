package com.chaekchaek.book.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.dto.BookMyRecordResponse;
import com.chaekchaek.book.dto.BookQuestionResponse;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BookDetailAssembler {

    private static final String DEFAULT_BOOK_SOURCE = "yes24";

    private final BookActivityCountReader activityCountReader;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final LibraryItemRepository libraryItemRepository;
    private final LibraryBookRepository libraryBookRepository;

    @Value("${app.book-source:yes24}")
    private String defaultBookSource = DEFAULT_BOOK_SOURCE;

    BookDetailResponse assemble(Book book) {
        Long bookId = book.getId();
        OptionalLong memberId = currentMemberIdProvider.findCurrentMemberId();
        if (bookId == null) {
            return new BookDetailResponse(
                    null,
                    book.getIsbn13().value(),
                    book.getTitle(),
                    book.getCoverImageUrl(),
                    book.getDescription(),
                    book.getAuthors(),
                    book.getTranslators(),
                    book.getPublisher(),
                    book.getCategory(),
                    book.getPublishedDate() == null ? null : book.getPublishedDate().toString(),
                    book.getTotalPages(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    isRegisteredInMyLibrary(bookId, memberId),
                    book.getPurchaseUrl(),
                    bookSource(book),
                    bookQuestion(book)
            );
        }
        Map<Long, ActivityCounts> activityCounts = activityCountReader.getActivityCounts(List.of(bookId));
        ActivityCounts counts = activityCounts.getOrDefault(bookId, ActivityCounts.ZERO);
        LibraryItemRepository.RatingStatistics ratings = libraryItemRepository
                .findRatingStatisticsByBookIdIn(List.of(bookId))
                .stream()
                .findFirst()
                .orElse(null);
        return new BookDetailResponse(
                bookId,
                book.getIsbn13().value(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getDescription(),
                book.getAuthors(),
                book.getTranslators(),
                book.getPublisher(),
                book.getCategory(),
                book.getPublishedDate() == null ? null : book.getPublishedDate().toString(),
                book.getTotalPages(),
                Math.toIntExact(counts.reviewCount()),
                Math.toIntExact(counts.replyCount()),
                averageRating(ratings),
                ratingCount(ratings),
                myRatingCount(memberId),
                myRecord(bookId, memberId),
                isRegisteredInMyLibrary(bookId, memberId),
                book.getPurchaseUrl(),
                bookSource(book),
                bookQuestion(book)
        );
    }

    private Boolean isRegisteredInMyLibrary(Long bookId, OptionalLong memberId) {
        if (memberId.isEmpty()) {
            return null;
        }
        return bookId != null
                && libraryBookRepository.countByMemberIdAndBookId(memberId.getAsLong(), bookId) > 0;
    }

    private String bookSource(Book book) {
        return book.getSource() == null ? defaultBookSource : book.getSource();
    }

    private BookQuestionResponse bookQuestion(Book book) {
        if (book.getQuestion() == null) {
            return null;
        }
        return new BookQuestionResponse(book.getQuestion(), book.getPlot(), book.getExtract());
    }

    private BigDecimal averageRating(LibraryItemRepository.RatingStatistics ratings) {
        if (ratings == null) {
            return null;
        }
        return BigDecimal.valueOf(ratings.getAverageRating()).setScale(1, RoundingMode.HALF_UP);
    }

    private Integer ratingCount(LibraryItemRepository.RatingStatistics ratings) {
        return ratings == null ? 0 : Math.toIntExact(ratings.getRatingCount());
    }

    private Integer myRatingCount(OptionalLong memberId) {
        if (memberId.isEmpty()) {
            return null;
        }
        return Math.toIntExact(libraryItemRepository.countByMemberIdAndRatingIsNotNull(memberId.getAsLong()));
    }

    private BookMyRecordResponse myRecord(long bookId, OptionalLong memberId) {
        if (memberId.isEmpty()) {
            return null;
        }
        return libraryItemRepository.findByMemberIdAndBookId(memberId.getAsLong(), bookId)
                .map(this::myRecord)
                .orElse(null);
    }

    private BookMyRecordResponse myRecord(LibraryItem item) {
        if (!item.hasReadingState() && item.getRating() == null) {
            return null;
        }
        String status = item.getStatus() == null ? null : item.getStatus().name();
        return new BookMyRecordResponse(
                status,
                item.getCurrentPage(),
                item.getRating()
        );
    }
}
