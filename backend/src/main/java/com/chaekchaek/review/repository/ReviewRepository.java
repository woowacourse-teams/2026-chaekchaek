package com.chaekchaek.review.repository;

import com.chaekchaek.review.domain.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByBookId(long bookId, Pageable pageable);

    Page<Review> findByBookIdAndMemberId(long bookId, long memberId, Pageable pageable);

    List<Review> findByBookId(long bookId);

    List<Review> findByBookIdAndMemberId(long bookId, long memberId);
}
