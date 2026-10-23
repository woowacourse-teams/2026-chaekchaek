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
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LibraryBookLibraryDeletionServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long LIBRARY_ID = 7L;
    private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("책장이 없으면 소유권을 확인하지 못해 배치 조회 없이 오류를 반환한다")
    void should_ReturnNotFound_When_LibraryDoesNotExist() {
        // given
        TestContext context = context(null, List.of(), List.of(), List.of());
        when(context.libraryRepository().findById(LIBRARY_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND)
                );
        verifyNoInteractions(context.libraryBookRepository());
        verify(context.libraryRepository(), never()).delete(org.mockito.ArgumentMatchers.any(Library.class));
    }

    @Test
    @DisplayName("다른 회원의 책장이면 배치 조회 전에 소유권 오류를 반환한다")
    void should_RejectForeignLibraryBeforeReadingPlacements() {
        // given
        TestContext context = context(library(2L, false), List.of(), List.of(), List.of());

        // when & then
        assertThatThrownBy(() -> context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(context.libraryBookRepository());
        verify(context.libraryRepository(), never()).delete(org.mockito.ArgumentMatchers.any(Library.class));
    }

    @Test
    @DisplayName("기본 책장 삭제를 배치 조회 전에 거부한다")
    void should_RejectDefaultLibraryDeletionBeforeReadingPlacements() {
        // given
        TestContext context = context(library(MEMBER_ID, true), List.of(), List.of(), List.of());

        // when & then
        assertThatThrownBy(() -> context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.DEFAULT_LIBRARY_DELETE_NOT_ALLOWED)
                );
        verifyNoInteractions(context.libraryBookRepository());
        verify(context.libraryRepository(), never()).delete(org.mockito.ArgumentMatchers.any(Library.class));
    }

    @Test
    @DisplayName("활성 회원이 아니면 책장 조회 전에 삭제를 거부한다")
    void should_RejectInactiveMemberBeforeLookingUpLibrary() {
        // given
        TestContext context = context(library(MEMBER_ID, false), List.of(), List.of(), List.of());
        Member inactiveMember = activeMember();
        ReflectionTestUtils.setField(inactiveMember, "accountStatus", AccountStatus.SUSPENDED);
        when(context.memberRepository().findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(inactiveMember));

        // when & then
        assertThatThrownBy(() -> context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(context.libraryRepository(), context.libraryBookRepository());
    }

    @Test
    @DisplayName("비어 있는 일반 책장을 삭제한다")
    void should_DeleteEmptyLibrary_When_NoPlacementsExist() {
        // given
        Library library = library(MEMBER_ID, false);
        TestContext context = context(library, List.of(), List.of(), List.of());

        // when
        context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID);

        // then
        verify(context.libraryRepository()).delete(library);
        verify(context.libraryBookRepository()).findAllByLibraryId(LIBRARY_ID);
        verify(context.libraryBookRepository(), never()).findPlacedBookIdsByMemberIdAndBookIdIn(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyCollection()
        );
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("책장 배치를 일괄 삭제하고 마지막 배치 도서의 독서 상태만 초기화한다")
    void should_DeletePlacementsAndClearOnlyLastReadingStates() {
        // given
        Library library = library(MEMBER_ID, false);
        List<LibraryBook> placements = List.of(
                LibraryBook.create(LIBRARY_ID, 10L, 1L),
                LibraryBook.create(LIBRARY_ID, 20L, 2L),
                LibraryBook.create(LIBRARY_ID, 30L, 3L)
        );
        LibraryItem lastItem = libraryItem(10L);
        Instant addedAt = lastItem.getAddedAt();
        Instant ratingUpdatedAt = lastItem.getRatingUpdatedAt();
        TestContext context = context(library, placements, List.of(20L, 30L), List.of(lastItem));

        // when
        context.service().deleteLibrary(MEMBER_ID, LIBRARY_ID);

        // then
        assertThat(lastItem.getStatus()).isNull();
        assertThat(lastItem.getCurrentPage()).isNull();
        assertThat(lastItem.getReadingUpdatedAt()).isNull();
        assertThat(lastItem.getRating()).isEqualByComparingTo("4.5");
        assertThat(lastItem.getRatingUpdatedAt()).isEqualTo(ratingUpdatedAt);
        assertThat(lastItem.getAddedAt()).isEqualTo(addedAt);
        InOrder order = inOrder(
                context.memberRepository(),
                context.libraryRepository(),
                context.libraryBookRepository(),
                context.libraryItemRepository()
        );
        order.verify(context.memberRepository()).findByIdForUpdate(MEMBER_ID);
        order.verify(context.libraryRepository()).findById(LIBRARY_ID);
        order.verify(context.libraryBookRepository()).findAllByLibraryId(LIBRARY_ID);
        order.verify(context.libraryBookRepository()).deleteAll(placements);
        order.verify(context.libraryBookRepository()).flush();
        order.verify(context.libraryBookRepository()).findPlacedBookIdsByMemberIdAndBookIdIn(
                MEMBER_ID,
                List.of(10L, 20L, 30L)
        );
        order.verify(context.libraryItemRepository()).findAllByMemberIdAndBookIdInForUpdate(
                MEMBER_ID,
                List.of(10L)
        );
        order.verify(context.libraryRepository()).delete(library);
    }

    private TestContext context(
            Library library,
            List<LibraryBook> placements,
            List<Long> remainingBookIds,
            List<LibraryItem> lastItems
    ) {
        LibraryRepository libraryRepository = mock(LibraryRepository.class);
        LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(libraryRepository.findById(LIBRARY_ID)).thenReturn(Optional.ofNullable(library));
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(activeMember()));
        when(libraryBookRepository.findAllByLibraryId(LIBRARY_ID)).thenReturn(placements);
        when(libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(MEMBER_ID, List.of(10L, 20L, 30L)))
                .thenReturn(remainingBookIds);
        when(libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(MEMBER_ID, List.of(10L)))
                .thenReturn(lastItems);
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

    private Library library(long memberId, boolean isDefault) {
        Library library = Library.create(memberId, "가을책장", NOW);
        ReflectionTestUtils.setField(library, "id", LIBRARY_ID);
        ReflectionTestUtils.setField(library, "isDefault", isDefault);
        return library;
    }

    private LibraryItem libraryItem(long bookId) {
        LibraryItem item = LibraryItem.create(MEMBER_ID, bookId, ReadingStatus.READING, 368, NOW.minusSeconds(120));
        item.changeCurrentPage(120, 368, NOW.minusSeconds(60));
        item.rate(new BigDecimal("4.5"), NOW.minusSeconds(30));
        return item;
    }

    private Member activeMember() {
        return Member.create("활성 책장 삭제 회원", null, LocalDateTime.of(2026, 10, 23, 3, 0));
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
