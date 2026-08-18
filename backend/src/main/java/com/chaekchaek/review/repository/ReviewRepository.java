package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByBookId(long bookId, Pageable pageable);

    Page<Review> findByBookIdAndMemberId(long bookId, long memberId, Pageable pageable);

    List<Review> findByBookId(long bookId);

    List<Review> findByBookIdAndMemberId(long bookId, long memberId);

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
