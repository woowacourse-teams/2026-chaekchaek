package com.chaekchaek.review.config;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.review.exception.ReviewErrorCode;
import com.chaekchaek.review.exception.ReviewException;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Temporary application-boundary defaults until Auth and Library provide their implementations. */
@Configuration
public class ReviewBoundaryConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentMemberIdProvider.class)
    CurrentMemberIdProvider currentMemberIdProvider() {
        return () -> { throw new ReviewException(ReviewErrorCode.UNAUTHORIZED); };
    }

    @Bean
    @ConditionalOnMissingBean(ReadingRecordCoordinator.class)
    ReadingRecordCoordinator readingRecordCoordinator() {
        return (memberId, bookId, currentPage, totalPages) -> { };
    }
}
