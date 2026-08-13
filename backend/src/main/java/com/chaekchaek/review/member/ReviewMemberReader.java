package com.chaekchaek.review.member;

import java.util.Collection;
import java.util.Map;

/**
 * Member/Auth projection boundary. Anonymous preference is snapshotted at writing time, while
 * display name and profile image remain current at read time.
 */
public interface ReviewMemberReader {

    Map<Long, ReviewMemberProfile> findByMemberIds(Collection<Long> memberIds);
}
