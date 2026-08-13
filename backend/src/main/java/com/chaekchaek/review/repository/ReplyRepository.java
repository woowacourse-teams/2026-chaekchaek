package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.Reply;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    Page<Reply> findByReviewId(long reviewId, Pageable pageable);

    List<Reply> findByReviewIdInOrderByCreatedAtDescIdDesc(Collection<Long> reviewIds);

    @Query("select r.reviewId as reviewId, count(r) as count from Reply r where r.reviewId in :reviewIds group by r.reviewId")
    List<ReplyCount> countByReviewIdInGroupByReviewId(Collection<Long> reviewIds);

    interface ReplyCount {
        long getReviewId();
        long getCount();
    }
}
