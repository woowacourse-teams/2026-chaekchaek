package com.chaekchaek.library.service;

import java.util.Collection;
import java.util.Map;

public interface BookActivityCountReader {

    Map<Long, ActivityCounts> getActivityCounts(Collection<Long> bookIds);

    record ActivityCounts(long reviewCount, long replyCount) {

        public long totalCount() {
            return reviewCount + replyCount;
        }
    }
}
