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

    interface BookCommentCount {
        long getBookId();
        long getCount();
    }
}
