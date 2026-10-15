package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByBookId(long bookId, Pageable pageable);

    Page<Review> findByBookIdAndActorId(long bookId, long actorId, Pageable pageable);

    List<Review> findByBookId(long bookId);

    List<Review> findByBookIdAndActorId(long bookId, long actorId);

    long countByBookId(long bookId);

    long countByBookIdAndActorId(long bookId, long actorId);

    @Query(value = """
            select r.*
            from review r
            left join (
                select rc.review_id, count(*) as cnt
                from reaction rc
                join review br on br.review_id = rc.review_id
                where br.book_id = :bookId
                group by rc.review_id
            ) reaction_counts on reaction_counts.review_id = r.review_id
            left join (
                select rp.review_id, count(*) as cnt
                from reply rp
                join review br on br.review_id = rp.review_id
                where br.book_id = :bookId
                group by rp.review_id
            ) reply_counts on reply_counts.review_id = r.review_id
            where r.book_id = :bookId
            order by coalesce(reaction_counts.cnt, 0) + coalesce(reply_counts.cnt, 0) desc, r.review_id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Review> findPopularByBookId(long bookId, int limit, long offset);

    @Query(value = """
            select r.*
            from review r
            left join (
                select rc.review_id, count(*) as cnt
                from reaction rc
                join review br on br.review_id = rc.review_id
                where br.book_id = :bookId and br.actor_id = :actorId
                group by rc.review_id
            ) reaction_counts on reaction_counts.review_id = r.review_id
            left join (
                select rp.review_id, count(*) as cnt
                from reply rp
                join review br on br.review_id = rp.review_id
                where br.book_id = :bookId and br.actor_id = :actorId
                group by rp.review_id
            ) reply_counts on reply_counts.review_id = r.review_id
            where r.book_id = :bookId and r.actor_id = :actorId
            order by coalesce(reaction_counts.cnt, 0) + coalesce(reply_counts.cnt, 0) desc, r.review_id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Review> findPopularByBookIdAndActorId(
            long bookId,
            long actorId,
            int limit,
            long offset
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Review r set r.actorId = :targetActorId where r.actorId = :sourceActorId")
    int reassignActorId(long sourceActorId, long targetActorId);

    List<Review> findTop10ByDeletedAtIsNullAndSpoilerFalseOrderByCreatedAtDescIdDesc();

    @Query(value = """
            select r from Review r
            where r.deletedAt is null
              and exists (select b.id from Book b where b.id = r.bookId)
            """, countQuery = """
            select count(r) from Review r
            where r.deletedAt is null
              and exists (select b.id from Book b where b.id = r.bookId)
            """)
    Page<Review> findFeedReviews(Pageable pageable);

    @Query("select r.bookId as bookId, count(r) as count from Review r where r.bookId in :bookIds group by r.bookId")
    List<BookCommentCount> countByBookIdInGroupByBookId(Collection<Long> bookIds);

    @Query(value = """
            select r.book_id as bookId,
                   count(distinct r.review_id) as reviewCount,
                   count(reply.reply_id) as replyCount
            from review r
            left join reply on reply.review_id = r.review_id and reply.deleted_at is null
            where r.deleted_at is null
            group by r.book_id
            order by count(distinct r.review_id) + count(reply.reply_id) desc, r.book_id desc
            limit 10
            """, nativeQuery = true)
    List<PopularBookCount> findTop10PopularBookCounts();

    interface BookCommentCount {
        long getBookId();
        long getCount();
    }

    interface PopularBookCount {
        long getBookId();
        long getReviewCount();
        long getReplyCount();
    }
}
