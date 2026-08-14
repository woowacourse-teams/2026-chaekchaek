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
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    Optional<LibraryItem> findByMemberIdAndBookId(long memberId, long bookId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from LibraryItem item where item.memberId = :memberId and item.bookId = :bookId")
    Optional<LibraryItem> findByMemberIdAndBookIdForUpdate(
            @Param("memberId") long memberId,
            @Param("bookId") long bookId
    );

    List<LibraryItem> findAllByMemberIdAndBookIdIn(long memberId, Collection<Long> bookIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from LibraryItem item where item.memberId = :memberId and item.bookId in :bookIds")
    List<LibraryItem> findAllByMemberIdAndBookIdInForUpdate(
            @Param("memberId") long memberId,
            @Param("bookIds") Collection<Long> bookIds
    );

    Page<LibraryItem> findAllByMemberId(long memberId, Pageable pageable);

    List<LibraryItem> findAllByMemberId(long memberId);

    Page<LibraryItem> findAllByMemberIdAndStatus(long memberId, ReadingStatus status, Pageable pageable);

    List<LibraryItem> findAllByMemberIdAndStatus(long memberId, ReadingStatus status);

    long countByMemberId(long memberId);

    long countByMemberIdAndStatus(long memberId, ReadingStatus status);

    Optional<LibraryItem> findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
            long memberId, long bookId, BigDecimal rating);

    Optional<LibraryItem> findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
            long memberId, long bookId, BigDecimal rating);
}
