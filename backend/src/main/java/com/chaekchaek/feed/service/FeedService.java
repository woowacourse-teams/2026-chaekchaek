package com.chaekchaek.feed.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.feed.dto.FeedReviewListResponse;
import com.chaekchaek.feed.dto.FeedReviewResponse;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.repository.ReviewRepository;
import com.chaekchaek.review.service.ReviewSummaryReader;
import com.chaekchaek.review.service.ReviewSummaryReader.ReviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int PAGE_SIZE = 20;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final ReviewRepository reviewRepository;
    private final ReviewSummaryReader reviewSummaryReader;

    @Transactional(readOnly = true)
    public FeedReviewListResponse getReviews(int page) {
        Page<Review> reviews = reviewRepository.findFeedReviews(PageRequest.of(page - 1, PAGE_SIZE, LATEST_FIRST));
        Integer nextPage = null;
        if (reviews.hasNext()) {
            nextPage = page + 1;
        }
        return new FeedReviewListResponse(
                reviews.getTotalElements(),
                nextPage,
                reviewSummaryReader.read(reviews.getContent())
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    private FeedReviewResponse toResponse(ReviewSummary summary) {
        Review review = summary.review();
        Book book = summary.book();
        return new FeedReviewResponse(
                review.getId(),
                review.getContent(),
                review.isSpoiler(),
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
