package com.chaekchaek.book.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.dto.BookMyRecordResponse;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.service.BookCommentCountReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BookDetailAssembler {

    private final BookCommentCountReader commentCountReader;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final LibraryItemRepository libraryItemRepository;

    BookDetailResponse assemble(Book book) {
        Map<Long, Long> commentCounts = commentCountReader.getCommentCounts(java.util.List.of(book.getId()));
        LibraryItemRepository.RatingStatistics ratings = libraryItemRepository
                .findRatingStatisticsByBookIdIn(java.util.List.of(book.getId()))
                .stream()
                .findFirst()
                .orElse(null);
        return new BookDetailResponse(
                book.getId(), book.getIsbn13(), book.getTitle(), book.getCoverImageUrl(),
                book.getAuthors(), book.getTranslators(), book.getPublisher(), book.getCategory(),
                book.getPublishedDate() == null ? null : book.getPublishedDate().toString(),
                book.getTotalPages(), commentCounts.getOrDefault(book.getId(), 0L).intValue(),
                averageRating(ratings), ratingCount(ratings), myRecord(book.getId())
        );
    }

    private BigDecimal averageRating(LibraryItemRepository.RatingStatistics ratings) {
        if (ratings == null) {
            return null;
        }
        return BigDecimal.valueOf(ratings.getAverageRating()).setScale(1, RoundingMode.HALF_UP);
    }

    private int ratingCount(LibraryItemRepository.RatingStatistics ratings) {
        return ratings == null ? 0 : Math.toIntExact(ratings.getRatingCount());
    }

    private BookMyRecordResponse myRecord(long bookId) {
        OptionalLong memberId = currentMemberIdProvider.findCurrentMemberId();
        if (memberId.isEmpty()) {
            return null;
        }
        return libraryItemRepository.findByMemberIdAndBookId(memberId.getAsLong(), bookId)
                .map(item -> new BookMyRecordResponse(
                        item.getStatus().name(), item.getCurrentPage(), item.getRating()))
                .orElse(null);
    }
}
