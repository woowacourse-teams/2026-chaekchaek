package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    Optional<LibraryItem> findByMemberIdAndBookId(long memberId, long bookId);

    List<LibraryItem> findAllByMemberIdAndBookIdIn(long memberId, Collection<Long> bookIds);

    Page<LibraryItem> findAllByMemberId(long memberId, Pageable pageable);

    Page<LibraryItem> findAllByMemberIdAndStatus(long memberId, ReadingStatus status, Pageable pageable);

    long countByMemberId(long memberId);

    long countByMemberIdAndStatus(long memberId, ReadingStatus status);

    Optional<LibraryItem> findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
            long memberId, long bookId, BigDecimal rating);

    Optional<LibraryItem> findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
            long memberId, long bookId, BigDecimal rating);
}
