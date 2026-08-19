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

    @Query(value = """
            select recent_replies.*
            from (
                select r.*, row_number() over (
                    partition by r.review_id order by r.created_at desc, r.reply_id desc
                ) as reply_rank
                from reply r
                where r.review_id in (:reviewIds)
            ) recent_replies
            where recent_replies.reply_rank <= 3
            order by recent_replies.review_id, recent_replies.created_at asc, recent_replies.reply_id asc
            """, nativeQuery = true)
    List<Reply> findRecentThreeByReviewIdIn(Collection<Long> reviewIds);

    @Query("select r.reviewId as reviewId, count(r) as count from Reply r where r.reviewId in :reviewIds group by r.reviewId")
    List<ReplyCount> countByReviewIdInGroupByReviewId(Collection<Long> reviewIds);

    @Query("""
            select r.reviewId as reviewId, count(r) as count
            from Reply r join Review review on review.id = r.reviewId
            where r.reviewId in :reviewIds and r.deletedAt is null and review.deletedAt is null
            group by r.reviewId
            """)
    List<ReviewCount> countActiveByReviewIdInGroupByReviewId(Collection<Long> reviewIds);

    @Query("""
            select review.bookId as bookId, count(reply) as count
            from Reply reply join Review review on review.id = reply.reviewId
            where review.bookId in :bookIds
            group by review.bookId
            """)
    List<BookCommentCount> countByReviewBookIdInGroupByBookId(Collection<Long> bookIds);

    interface ReplyCount {
        long getReviewId();
        long getCount();
    }

    interface ReviewCount {
        long getReviewId();
        long getCount();
    }

    interface BookCommentCount {
        long getBookId();
        long getCount();
    }
}
