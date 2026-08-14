package com.chaekchaek.library.service;

import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.Collection;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Keeps Library independently deployable until Review supplies its aggregate reader.
 */
class EmptyBookCommentCountReader implements BookCommentCountReader {

    @Override
    public Map<Long, Long> getCommentCounts(Collection<Long> bookIds) {
        return Map.of();
    }
}

@Configuration
class BookCommentCountReaderConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentMemberIdProvider.class)
    CurrentMemberIdProvider currentMemberIdProvider() {
        return () -> {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        };
    }

    @Bean
    @ConditionalOnMissingBean(BookCommentCountReader.class)
    BookCommentCountReader emptyBookCommentCountReader() {
        return new EmptyBookCommentCountReader();
    }
}
