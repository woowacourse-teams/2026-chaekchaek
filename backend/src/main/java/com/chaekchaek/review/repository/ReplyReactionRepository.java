package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.ReplyReaction;
import com.chaekchaek.review.domain.ReplyReaction.ReplyReactionId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReplyReactionRepository extends JpaRepository<ReplyReaction, ReplyReactionId> {

    long countByReplyId(long replyId);

    @Query("select r.replyId as replyId, count(r) as count from ReplyReaction r where r.replyId in :replyIds group by r.replyId")
    List<ReactionCount> countByReplyIdInGroupByReplyId(Collection<Long> replyIds);

    List<ReplyReaction> findByReplyIdInAndActorId(Collection<Long> replyIds, long actorId);

    @Modifying
    @Query(value = """
            delete from reply_reaction
            where actor_id = :sourceActorId
              and reply_id in (
                  select reply_id from reply_reaction where actor_id = :targetActorId
              )
            """, nativeQuery = true)
    int deleteConflictingReactions(long sourceActorId, long targetActorId);

    @Modifying
    @Query(value = "update reply_reaction set actor_id = :targetActorId where actor_id = :sourceActorId",
            nativeQuery = true)
    int reassignActorId(long sourceActorId, long targetActorId);

    interface ReactionCount {
        long getReplyId();
        long getCount();
    }
}
