package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.fixture.MemberFixture;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LibraryBookDeletionServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long LIBRARY_ID = 7L;
    private static final long BOOK_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-10-23T02:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("책장 소유권을 확인하기 전에 배치 존재 여부를 조회하지 않는다")
    void should_RejectForeignLibraryBeforeCheckingPlacement() {
        // given
        TestContext context = context(library(2L), Optional.empty(), 0L, null);

        // when & then
        assertThatThrownBy(() -> context.service().delete(MEMBER_ID, LIBRARY_ID, BOOK_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(context.libraryBookRepository());
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("책장이 없으면 배치 조회 전에 책장을 찾을 수 없음 오류를 반환한다")
    void should_ReturnNotFound_When_LibraryDoesNotExist() {
        // given
        TestContext context = context(null, Optional.empty(), 0L, null);
        when(context.libraryRepository().findById(LIBRARY_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> context.service().delete(MEMBER_ID, LIBRARY_ID, BOOK_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND)
                );
        verifyNoInteractions(context.libraryBookRepository());
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("배치가 없으면 공유 독서 기록을 바꾸지 않고 종료한다")
    void should_DoNothing_When_PlacementDoesNotExist() {
        // given
        TestContext context = context(library(MEMBER_ID), Optional.empty(), 0L, null);

        // when
        context.service().delete(MEMBER_ID, LIBRARY_ID, BOOK_ID);

        // then
        verify(context.libraryBookRepository(), never()).delete(org.mockito.ArgumentMatchers.any());
        verify(context.libraryBookRepository(), never()).countByMemberIdAndBookId(MEMBER_ID, BOOK_ID);
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("다른 책장에 배치가 남아 있으면 공유 독서 기록을 유지한다")
    void should_PreserveSharedReadingRecord_When_AnotherPlacementRemains() {
        // given
        LibraryBook placement = LibraryBook.create(LIBRARY_ID, BOOK_ID, 1L);
        TestContext context = context(library(MEMBER_ID), Optional.of(placement), 1L, null);

        // when
        context.service().delete(MEMBER_ID, LIBRARY_ID, BOOK_ID);

        // then
        InOrder order = inOrder(context.libraryBookRepository());
        order.verify(context.libraryBookRepository()).delete(placement);
        order.verify(context.libraryBookRepository()).flush();
        verify(context.libraryBookRepository()).countByMemberIdAndBookId(MEMBER_ID, BOOK_ID);
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("마지막 배치를 삭제하면 독서 상태만 초기화하고 별점과 최초 시각을 보존한다")
    void should_ClearReadingStateAndPreserveRating_When_DeletingLastPlacement() {
        // given
        LibraryBook placement = LibraryBook.create(LIBRARY_ID, BOOK_ID, 1L);
        LibraryItem item = LibraryItem.create(MEMBER_ID, BOOK_ID, ReadingStatus.READING, 368, NOW.minusSeconds(120));
        item.changeCurrentPage(120, 368, NOW.minusSeconds(60));
        item.rate(new BigDecimal("4.5"), NOW.minusSeconds(30));
        Instant addedAt = item.getAddedAt();
        Instant ratingUpdatedAt = item.getRatingUpdatedAt();
        TestContext context = context(library(MEMBER_ID), Optional.of(placement), 0L, item);

        // when
        context.service().delete(MEMBER_ID, LIBRARY_ID, BOOK_ID);

        // then
        assertThat(item.getStatus()).isNull();
        assertThat(item.getCurrentPage()).isNull();
        assertThat(item.getReadingUpdatedAt()).isNull();
        assertThat(item.getRating()).isEqualByComparingTo("4.5");
        assertThat(item.getRatingUpdatedAt()).isEqualTo(ratingUpdatedAt);
        assertThat(item.getAddedAt()).isEqualTo(addedAt);
        InOrder order = inOrder(context.memberRepository(), context.libraryRepository(),
                context.libraryBookRepository(), context.libraryItemRepository());
        order.verify(context.memberRepository()).findByIdForUpdate(MEMBER_ID);
        order.verify(context.libraryRepository()).findById(LIBRARY_ID);
        order.verify(context.libraryBookRepository()).findByLibraryIdAndBookId(LIBRARY_ID, BOOK_ID);
        order.verify(context.libraryBookRepository()).delete(placement);
        order.verify(context.libraryBookRepository()).flush();
        order.verify(context.libraryBookRepository()).countByMemberIdAndBookId(MEMBER_ID, BOOK_ID);
        order.verify(context.libraryItemRepository()).findByMemberIdAndBookIdForUpdate(MEMBER_ID, BOOK_ID);
    }

    private TestContext context(
            Library library,
            Optional<LibraryBook> placement,
            long remainingPlacements,
            LibraryItem item
    ) {
        LibraryRepository libraryRepository = mock(LibraryRepository.class);
        LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(libraryRepository.findById(LIBRARY_ID)).thenReturn(Optional.ofNullable(library));
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(MemberFixture.member()));
        when(libraryBookRepository.findByLibraryIdAndBookId(LIBRARY_ID, BOOK_ID)).thenReturn(placement);
        when(libraryBookRepository.countByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(remainingPlacements);
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(MEMBER_ID, BOOK_ID))
                .thenReturn(Optional.ofNullable(item));
        return new TestContext(
                new LibraryBookService(
                        libraryRepository,
                        libraryBookRepository,
                        libraryItemRepository,
                        mock(BookRepository.class),
                        mock(BookResolver.class),
                        mock(BookCommentCountReader.class),
                        memberRepository,
                        CLOCK,
                        transactionManager()
                ),
                libraryRepository,
                libraryBookRepository,
                libraryItemRepository,
                memberRepository
        );
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any(TransactionDefinition.class)))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        return transactionManager;
    }

    private Library library(long memberId) {
        Library library = Library.create(memberId, "가을책장", NOW);
        ReflectionTestUtils.setField(library, "id", LIBRARY_ID);
        return library;
    }

    private record TestContext(
            LibraryBookService service,
            LibraryRepository libraryRepository,
            LibraryBookRepository libraryBookRepository,
            LibraryItemRepository libraryItemRepository,
            MemberRepository memberRepository
    ) {
    }
}
