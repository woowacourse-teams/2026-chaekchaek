package com.chaekchaek.library.service;

import java.util.Collection;
import java.util.Map;

public interface BookActivityCountReader {

    Map<Long, ActivityCounts> getActivityCounts(Collection<Long> bookIds);

    record ActivityCounts(long reviewCount, long replyCount) {

        public static final ActivityCounts ZERO = new ActivityCounts(0L, 0L);

        public long totalCount() {
            return reviewCount + replyCount;
        }
    }
}
