package com.chaekchaek.home.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.home.dto.LatestReviewListResponse;
import com.chaekchaek.home.dto.LatestReviewResponse;
import com.chaekchaek.home.dto.PopularBookListResponse;
import com.chaekchaek.home.dto.PopularBookResponse;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewSummaryReader;
import com.chaekchaek.review.service.ReviewSummaryReader.ReviewSummary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ReviewSummaryReader reviewSummaryReader;

    @Transactional(readOnly = true)
    public PopularBookListResponse getPopularBooks() {
        List<ReviewRepository.PopularBookCount> popularBookCounts = reviewRepository.findTop10PopularBookCounts();
        List<Long> bookIds = popularBookCounts.stream()
                .map(ReviewRepository.PopularBookCount::getBookId)
                .toList();
        Map<Long, Book> books = booksWithAuthorsById(bookIds);
        List<PopularBookResponse> responses = popularBookCounts.stream()
                .map(count -> toPopularBookResponse(books.get(count.getBookId()), count))
                .filter(Objects::nonNull)
                .toList();
        return new PopularBookListResponse(responses);
    }

    private Map<Long, Book> booksWithAuthorsById(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllWithAuthorsByIdIn(bookIds)
                .stream()
                .collect(Collectors.toMap(Book::getId, book -> book));
    }

    private PopularBookResponse toPopularBookResponse(Book book, ReviewRepository.PopularBookCount count) {
        if (book == null) {
            return null;
        }
        long bookId = book.getId();
        return new PopularBookResponse(
                bookId,
                book.getIsbn13().value(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors(),
                count.getReviewCount(),
                count.getReplyCount()
        );
    }

    @Transactional(readOnly = true)
    public LatestReviewListResponse getLatestReviews() {
        List<Review> reviews = reviewRepository.findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc();
        List<LatestReviewResponse> responses = reviewSummaryReader.read(reviews)
                .stream()
                .map(this::toLatestReviewResponse)
                .toList();
        return new LatestReviewListResponse(responses);
    }

    private LatestReviewResponse toLatestReviewResponse(ReviewSummary summary) {
        Review review = summary.review();
        Book book = summary.book();
        return new LatestReviewResponse(
                review.getContent(),
                review.getCreatedAt(),
                summary.author(),
                summary.replyCount(),
                book.getId(),
                book.getIsbn13().value(),
                book.getTitle(),
                book.getCoverImageUrl()
        );
    }
}
