package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class LibraryOrderServiceTest {

    private final MemberRepository members = mock(MemberRepository.class);
    private final LibraryRepository libraries = mock(LibraryRepository.class);
    private final LibraryBookRepository placements = mock(LibraryBookRepository.class);
    private final LibraryOrderService service = new LibraryOrderService(members, libraries, placements);

    @BeforeEach
    void setUp() {
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(
                Member.create("익명", null, LocalDateTime.of(2026, 10, 23, 0, 0))
        ));
    }

    @Test
    @DisplayName("회원 잠금 후 기본 책장을 포함한 전체 책장 순서를 갱신한다")
    void should_ReorderAllLibraries_When_IdsMatch() {
        // given
        Library first = library(1L, 1L, 1L);
        ReflectionTestUtils.setField(first, "isDefault", true);
        Library second = library(2L, 1L, 8L);
        when(libraries.findAllByMemberIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(first, second));

        // when
        service.reorderLibraries(1L, List.of(2L, 1L));

        // then
        assertThat(first.getDisplayOrder()).isEqualTo(2L);
        assertThat(second.getDisplayOrder()).isEqualTo(1L);
        InOrder calls = inOrder(members, libraries);
        calls.verify(members).findByIdForUpdate(1L);
        calls.verify(libraries).findAllByMemberIdOrderByDisplayOrderAscIdAsc(1L);
        verifyNoInteractions(placements);
    }

    @Test
    @DisplayName("책장 전체 집합이 다르면 기존 순서를 전부 유지한다")
    void should_PreserveAllLibraryOrders_When_IdsConflict() {
        // given
        Library first = library(1L, 1L, 3L);
        Library second = library(2L, 1L, 9L);
        when(libraries.findAllByMemberIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(first, second));

        // when & then
        assertThatThrownBy(() -> service.reorderLibraries(1L, List.of(1L, 3L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_CONFLICT));
        assertThat(first.getDisplayOrder()).isEqualTo(3L);
        assertThat(second.getDisplayOrder()).isEqualTo(9L);
    }

    @Test
    @DisplayName("타인 책장은 배치와 요청 집합 확인보다 먼저 거절한다")
    void should_RejectForeignLibraryBeforeReadingPlacements_When_ReorderingBooks() {
        // given
        when(libraries.findById(7L)).thenReturn(Optional.of(library(7L, 2L, 1L)));

        // when & then
        assertThatThrownBy(() -> service.reorderBooks(1L, 7L, List.of(10L, 10L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(placements);
    }

    @Test
    @DisplayName("없는 책장은 배치 조회보다 먼저 거절한다")
    void should_RejectMissingLibrary_When_ReorderingBooks() {
        // when & then
        assertThatThrownBy(() -> service.reorderBooks(1L, 7L, List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND));
        verifyNoInteractions(placements);
    }

    @Test
    @DisplayName("이전된 35권도 전체 순서를 변경하고 같은 집합의 후속 요청은 덮어쓴다")
    void should_ApplyLastOrder_When_SameBookIdsAreSubmittedAgain() {
        // given
        List<Long> ids = LongStream.rangeClosed(1L, 35L)
                .boxed()
                .toList();
        List<LibraryBook> books = ids.stream()
                .map(id -> LibraryBook.create(7L, id, id * 2))
                .toList();
        when(libraries.findById(7L)).thenReturn(Optional.of(library(7L, 1L, 1L)));
        when(placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(7L)).thenReturn(books);

        // when
        service.reorderBooks(1L, 7L, ids.reversed());
        assertThat(books.getFirst().getDisplayOrder()).isEqualTo(35L);
        service.reorderBooks(1L, 7L, ids);

        // then
        assertThat(books).extracting(LibraryBook::getDisplayOrder).containsExactlyElementsOf(ids);
        InOrder calls = inOrder(members, libraries, placements);
        calls.verify(members).findByIdForUpdate(1L);
        calls.verify(libraries).findById(7L);
        calls.verify(placements).findAllByLibraryIdOrderByDisplayOrderAscIdAsc(7L);
    }

    @Test
    @DisplayName("도서 전체 집합이 다르면 배치 순서를 전부 유지한다")
    void should_PreserveAllBookOrders_When_IdsConflict() {
        // given
        LibraryBook first = LibraryBook.create(7L, 10L, 4L);
        LibraryBook second = LibraryBook.create(7L, 20L, 8L);
        when(libraries.findById(7L)).thenReturn(Optional.of(library(7L, 1L, 1L)));
        when(placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(7L)).thenReturn(List.of(first, second));

        // when & then
        assertThatThrownBy(() -> service.reorderBooks(1L, 7L, List.of(20L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_CONFLICT));
        assertThat(first.getDisplayOrder()).isEqualTo(4L);
        assertThat(second.getDisplayOrder()).isEqualTo(8L);
    }

    @Test
    @DisplayName("비활성 회원은 책장 조회 전에 거절한다")
    void should_RejectInactiveMember_When_ReorderingLibraries() {
        // given
        Member member = Member.create("익명", null, LocalDateTime.of(2026, 10, 23, 0, 0));
        ReflectionTestUtils.setField(member, "accountStatus", AccountStatus.SUSPENDED);
        when(members.findByIdForUpdate(1L)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> service.reorderLibraries(1L, List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(libraries, placements);
    }

    private Library library(long id, long memberId, long position) {
        Library library = Library.create(memberId, "책장" + id, Instant.parse("2026-10-23T00:00:00Z"));
        ReflectionTestUtils.setField(library, "id", id);
        library.changeDisplayOrder(position);
        return library;
    }
}
