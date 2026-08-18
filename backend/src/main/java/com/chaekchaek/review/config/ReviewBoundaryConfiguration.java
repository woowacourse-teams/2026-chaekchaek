package com.chaekchaek.review.config;

import com.chaekchaek.review.member.ReviewMemberProfile;
import com.chaekchaek.review.member.ReviewMemberReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Temporary application-boundary default until Member provides its implementation. */
@Configuration
public class ReviewBoundaryConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReviewMemberReader.class)
    ReviewMemberReader reviewMemberReader() {
        return memberIds -> memberIds.stream().collect(java.util.stream.Collectors.toMap(
                memberId -> memberId,
                this::defaultMemberProfile
        ));
    }

    private ReviewMemberProfile defaultMemberProfile(long memberId) {
        return new ReviewMemberProfile("회원-" + memberId, null, "익명-" + memberId, false, false);
    }
}
