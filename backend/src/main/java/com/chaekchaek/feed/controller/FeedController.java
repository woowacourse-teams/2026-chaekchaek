package com.chaekchaek.feed.controller;

import com.chaekchaek.feed.dto.FeedReviewListResponse;
import com.chaekchaek.feed.service.FeedService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/reviews")
    public ResponseEntity<FeedReviewListResponse> getReviews(@RequestParam @Positive int page) {
        return ResponseEntity.ok(feedService.getReviews(page));
    }
}
