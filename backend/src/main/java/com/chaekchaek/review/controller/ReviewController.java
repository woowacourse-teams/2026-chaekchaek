package com.chaekchaek.review.controller;

import com.chaekchaek.review.dto.PageResponse;
import com.chaekchaek.review.dto.ReactionResponse;
import com.chaekchaek.review.dto.ReplyCreateRequest;
import com.chaekchaek.review.dto.ReplyResponse;
import com.chaekchaek.review.dto.ReplyUpdateRequest;
import com.chaekchaek.review.dto.ReviewCreateRequest;
import com.chaekchaek.review.dto.ReviewResponse;
import com.chaekchaek.review.dto.ReviewUpdateRequest;
import com.chaekchaek.review.service.ReviewService;
import com.chaekchaek.review.service.ReviewService.Feed;
import com.chaekchaek.review.service.ReviewService.ReviewSort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> findReviews(@PathVariable long bookId,
                                                                      @RequestParam @Positive int page,
                                                                      @RequestParam(defaultValue = "ALL") Feed feed,
                                                                      @RequestParam(defaultValue = "PAGE") ReviewSort sort) {
        return ResponseEntity.ok(reviewService.findReviews(bookId, page, feed, sort));
    }

    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(@PathVariable long bookId,
                                                        @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.createReview(bookId, request);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + response.reviewId())).body(response);
    }

    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable long reviewId,
                                                        @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<PageResponse<ReplyResponse>> findReplies(@PathVariable long reviewId,
                                                                     @RequestParam @Positive int page) {
        return ResponseEntity.ok(reviewService.findReplies(reviewId, page));
    }

    @PostMapping("/reviews/{reviewId}/replies")
    public ResponseEntity<ReplyResponse> createReply(@PathVariable long reviewId,
                                                      @Valid @RequestBody ReplyCreateRequest request) {
        ReplyResponse response = reviewService.createReply(reviewId, request);
        return ResponseEntity.created(URI.create("/api/v1/replies/" + response.replyId())).body(response);
    }

    @PatchMapping("/replies/{replyId}")
    public ResponseEntity<ReplyResponse> updateReply(@PathVariable long replyId,
                                                      @Valid @RequestBody ReplyUpdateRequest request) {
        return ResponseEntity.ok(reviewService.updateReply(replyId, request));
    }

    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable long replyId) {
        reviewService.deleteReply(replyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{reviewId}/reactions")
    public ResponseEntity<ReactionResponse> createReviewReaction(@PathVariable long reviewId) {
        return ResponseEntity.status(201).body(reviewService.createReviewReaction(reviewId));
    }

    @DeleteMapping("/reviews/{reviewId}/reactions")
    public ResponseEntity<Void> deleteReviewReaction(@PathVariable long reviewId) {
        reviewService.deleteReviewReaction(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/replies/{replyId}/reactions")
    public ResponseEntity<ReactionResponse> createReplyReaction(@PathVariable long replyId) {
        return ResponseEntity.status(201).body(reviewService.createReplyReaction(replyId));
    }

    @DeleteMapping("/replies/{replyId}/reactions")
    public ResponseEntity<Void> deleteReplyReaction(@PathVariable long replyId) {
        reviewService.deleteReplyReaction(replyId);
        return ResponseEntity.noContent().build();
    }
}
