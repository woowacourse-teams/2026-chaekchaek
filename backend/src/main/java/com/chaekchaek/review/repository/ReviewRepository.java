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

    @Query(value = """
            select r from Review r
            where r.bookId = :bookId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """, countQuery = """
            select count(r) from Review r
            where r.bookId = :bookId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """)
    Page<Review> findVisibleByBookId(long bookId, Pageable pageable);

    @Query(value = """
            select r from Review r
            where r.bookId = :bookId and r.actorId = :actorId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """, countQuery = """
            select count(r) from Review r
            where r.bookId = :bookId and r.actorId = :actorId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """)
    Page<Review> findVisibleByBookIdAndActorId(long bookId, long actorId, Pageable pageable);

    List<Review> findByBookId(long bookId);

    List<Review> findByBookIdAndActorId(long bookId, long actorId);

    @Query("""
            select count(r) from Review r
            where r.bookId = :bookId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """)
    long countVisibleByBookId(long bookId);

    @Query("""
            select count(r) from Review r
            where r.bookId = :bookId and r.actorId = :actorId
              and (r.deletedAt is null
                   or exists (select rp.id from Reply rp where rp.reviewId = r.id and rp.deletedAt is null))
            """)
    long countVisibleByBookIdAndActorId(long bookId, long actorId);

    @Query(value = """
            select r.*
            from review r
            where r.book_id = :bookId
              and (r.deleted_at is null
                   or exists (select 1 from reply ar where ar.review_id = r.review_id and ar.deleted_at is null))
            order by (select count(*) from reaction rc where rc.review_id = r.review_id)
                    + (select count(*) from reply rp where rp.review_id = r.review_id and rp.deleted_at is null) desc,
                    r.review_id desc
            limit :limit offset :offset
            """, nativeQuery = true)
    List<Review> findPopularByBookId(long bookId, int limit, long offset);

    @Query(value = """
            select r.*
            from review r
            where r.book_id = :bookId and r.actor_id = :actorId
              and (r.deleted_at is null
                   or exists (select 1 from reply ar where ar.review_id = r.review_id and ar.deleted_at is null))
            order by (select count(*) from reaction rc where rc.review_id = r.review_id)
                    + (select count(*) from reply rp where rp.review_id = r.review_id and rp.deleted_at is null) desc,
                    r.review_id desc
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
