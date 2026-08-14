package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.ReplyReaction.ReplyReactionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReplyReactionRepository extends JpaRepository<ReplyReaction, ReplyReactionId> {

    long countByReplyId(long replyId);

    @Query("select r.replyId as replyId, count(r) as count from ReplyReaction r where r.replyId in :replyIds group by r.replyId")
    List<ReactionCount> countByReplyIdInGroupByReplyId(Collection<Long> replyIds);

    List<ReplyReaction> findByReplyIdInAndMemberId(Collection<Long> replyIds, long memberId);

    interface ReactionCount {
        long getReplyId();
        long getCount();
    }
}
