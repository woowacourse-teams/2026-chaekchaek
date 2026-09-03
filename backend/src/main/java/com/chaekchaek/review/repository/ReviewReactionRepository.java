package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.ReviewReaction;
import com.chaekchaek.review.domain.ReviewReaction.ReviewReactionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, ReviewReactionId> {

    long countByReviewId(long reviewId);

    @Query("select r.reviewId as reviewId, count(r) as count from ReviewReaction r where r.reviewId in :reviewIds group by r.reviewId")
    List<ReactionCount> countByReviewIdInGroupByReviewId(Collection<Long> reviewIds);

    List<ReviewReaction> findByReviewIdInAndActorId(Collection<Long> reviewIds, long actorId);

    interface ReactionCount {
        long getReviewId();
        long getCount();
    }
}
