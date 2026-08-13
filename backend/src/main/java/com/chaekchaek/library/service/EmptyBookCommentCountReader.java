package com.chaekchaek.library.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Keeps Library independently deployable until Review supplies its aggregate reader.
 */
@Component
@ConditionalOnMissingBean(BookCommentCountReader.class)
class EmptyBookCommentCountReader implements BookCommentCountReader {

    @Override
    public Map<Long, Long> getCommentCounts(Collection<Long> bookIds) {
        return Map.of();
    }
}
