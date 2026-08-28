package com.chaekchaek.admin.repository;

import com.chaekchaek.admin.domain.RecommendedBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendedBookRepository extends JpaRepository<RecommendedBook, Long> {

    List<RecommendedBook> findAllByOrderByCreatedAtDescIdDesc();

    Optional<RecommendedBook> findByBookId(long bookId);

    boolean existsByBookId(long bookId);
}
