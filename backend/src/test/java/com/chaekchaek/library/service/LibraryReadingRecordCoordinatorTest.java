package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class LibraryReadingRecordCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-08-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("감상 작성 시 서재가 없으면 생성하고 입력한 페이지까지 진도를 전진한다")
    void should_CreateLibraryItemAndAdvanceProgress_When_RecordingReview() {
        // given
        Book book = book(null);
        BookRepository bookRepository = mock(BookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book));
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(1L, 2L))
                .thenReturn(Optional.empty());
        when(libraryItemRepository.save(any(LibraryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LibraryReadingRecordCoordinator coordinator = new LibraryReadingRecordCoordinator(
                bookRepository,
                libraryItemRepository,
                memberRepository,
                CLOCK
        );

        // when
        coordinator.recordReview(1L, 2L, 120, 308);

        // then
        ArgumentCaptor<LibraryItem> itemCaptor = ArgumentCaptor.forClass(LibraryItem.class);
        verify(libraryItemRepository).save(itemCaptor.capture());
        InOrder lockOrder = inOrder(memberRepository, bookRepository, libraryItemRepository);
        lockOrder.verify(memberRepository).findByIdForUpdate(1L);
        lockOrder.verify(bookRepository).findByIdForUpdate(2L);
        lockOrder.verify(libraryItemRepository).findByMemberIdAndBookIdForUpdate(1L, 2L);
        LibraryItem savedItem = itemCaptor.getValue();
        assertThat(book.getTotalPages()).isEqualTo(308);
        assertThat(savedItem.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(savedItem.getCurrentPage()).isEqualTo(120);
    }

    @Test
    @DisplayName("감상 페이지가 기존 진도보다 낮으면 서재 진도를 되돌리지 않는다")
    void should_NotRegressProgress_When_ReviewPageIsBehindLibraryProgress() {
        // given
        Book book = book(308);
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 308, NOW.minusSeconds(60));
        item.changeCurrentPage(200, 308, NOW.minusSeconds(60));
        BookRepository bookRepository = mock(BookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book));
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(1L, 2L))
                .thenReturn(Optional.of(item));
        LibraryReadingRecordCoordinator coordinator = new LibraryReadingRecordCoordinator(
                bookRepository,
                libraryItemRepository,
                memberRepository,
                CLOCK
        );

        // when
        coordinator.recordReview(1L, 2L, 120, 308);

        // then
        assertThat(item.getCurrentPage()).isEqualTo(200);
        assertThat(item.getReadingUpdatedAt()).isEqualTo(NOW.minusSeconds(60));
    }

    @Test
    @DisplayName("독서 상태가 초기화된 기록에 감상을 작성하면 독서를 다시 시작한다")
    void should_RestartReading_When_RecordingReviewForClearedReadingState() {
        // given
        Book book = book(308);
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 308, NOW.minusSeconds(60));
        item.rate(new BigDecimal("4.5"), NOW.minusSeconds(60));
        item.clearReadingState();
        BookRepository bookRepository = mock(BookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book));
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(1L, 2L))
                .thenReturn(Optional.of(item));
        LibraryReadingRecordCoordinator coordinator = new LibraryReadingRecordCoordinator(
                bookRepository,
                libraryItemRepository,
                memberRepository,
                CLOCK
        );

        // when
        coordinator.recordReview(1L, 2L, 120, 308);

        // then
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item.getCurrentPage()).isEqualTo(120);
        assertThat(item.getReadingUpdatedAt()).isEqualTo(NOW);
        assertThat(item.getRating()).isEqualByComparingTo("4.5");
    }

    @Test
    @DisplayName("감상 페이지가 전체 페이지를 초과하면 독서 상태 오류를 반환한다")
    void should_ThrowInvalidReadingState_When_ReviewPageExceedsTotalPages() {
        // given
        BookRepository bookRepository = mock(BookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book(308)));
        LibraryReadingRecordCoordinator coordinator = new LibraryReadingRecordCoordinator(
                bookRepository,
                libraryItemRepository,
                memberRepository,
                CLOCK
        );

        // when & then
        assertThatThrownBy(() -> coordinator.validateReviewPage(1L, 2L, 309, 308))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE)
        );
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"WITHDRAWN", "SUSPENDED"})
    @DisplayName("회원 잠금 후 비활성 상태면 감상 독서 기록을 변경하지 않는다")
    void should_RejectReadingChanges_When_LockedMemberIsInactive(AccountStatus status) {
        // given
        Member member = mock(Member.class);
        when(member.getAccountStatus()).thenReturn(status);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));
        BookRepository bookRepository = mock(BookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        LibraryReadingRecordCoordinator coordinator = new LibraryReadingRecordCoordinator(
                bookRepository,
                libraryItemRepository,
                memberRepository,
                CLOCK
        );

        // when & then
        assertThatThrownBy(() -> coordinator.recordReview(1L, 2L, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThatThrownBy(() -> coordinator.validateReviewPage(1L, 2L, 10, 100))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(bookRepository, libraryItemRepository);
    }

    private Member activeMember() {
        return Member.create("익명회원", null, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    private Book book(Integer totalPages) {
        return Book.create(
                new Isbn13("9788925568683"),
                "마션",
                "https://example.com/cover.jpg",
                "https://example.com/cover.jpg/side",
                "https://example.com/cover.jpg/back",
                null,
                List.of("앤디 위어"),
                List.of(),
                "알에이치코리아",
                "SF",
                null,
                totalPages,
                null,
                null
        );
    }
}
