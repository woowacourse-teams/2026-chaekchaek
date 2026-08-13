package com.chaekchaek.review.config;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Temporary application-boundary defaults until Auth, Book, and Library provide their implementations. */
@Configuration
public class ReviewBoundaryConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentMemberIdProvider.class)
    CurrentMemberIdProvider currentMemberIdProvider() {
        return () -> { throw new BusinessException(ErrorCode.UNAUTHORIZED); };
    }

    @Bean
    @ConditionalOnMissingBean(ReviewBookReader.class)
    ReviewBookReader reviewBookReader() {
        return bookId -> { throw new BusinessException(ErrorCode.BOOK_NOT_FOUND); };
    }

    @Bean
    @ConditionalOnMissingBean(ReviewMemberReader.class)
    ReviewMemberReader reviewMemberReader() {
        return memberIds -> memberIds.stream().collect(java.util.stream.Collectors.toMap(
                memberId -> memberId,
                this::defaultMemberProfile
        ));
    }

    @Bean
    @ConditionalOnMissingBean(ReadingRecordCoordinator.class)
    ReadingRecordCoordinator readingRecordCoordinator() {
        return (memberId, bookId, currentPage, totalPages) -> { };
    }

    private ReviewMemberProfile defaultMemberProfile(long memberId) {
        return new ReviewMemberProfile("회원-" + memberId, null, "익명-" + memberId, false, false);
    }
}
