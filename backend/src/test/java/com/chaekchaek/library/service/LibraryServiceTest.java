package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"),
            ZoneOffset.UTC);
    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookResolver bookResolver;

    @Mock
    private BookCommentCountReader commentCountReader;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("일괄 완독 대상 중 전체 페이지가 없는 책이 있으면 어느 항목도 변경하지 않는다")
    void should_NotChangeAnyItem_When_BulkFinishContainsUnknownTotalPages() {
        // given
        LibraryItem knownItem = LibraryItem.create(1L, 2L, ReadingStatus.READING, 100, CLOCK.instant());
        LibraryItem unknownItem = LibraryItem.create(1L, 3L, ReadingStatus.READING, null, CLOCK.instant());
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, List.of(2L, 3L)))
                .thenReturn(List.of(knownItem, unknownItem));
        when(libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(1L, List.of(2L, 3L)))
                .thenReturn(List.of(knownItem, unknownItem));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(java.util.Optional.of(book(100)));
        when(bookRepository.findByIdForUpdate(3L)).thenReturn(java.util.Optional.of(book(null)));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.bulkChangeStatus(1L, List.of(2L, 3L), ReadingStatus.FINISHED))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE));
        assertThat(knownItem.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(unknownItem.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("일괄 완독은 책을 먼저 잠근 뒤 서재 항목을 잠근다")
    void should_LockBooksBeforeLibraryItems_When_BulkFinishing() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 100, CLOCK.instant());
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, List.of(2L)))
                .thenReturn(List.of(item));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(java.util.Optional.of(book(100)));
        when(libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(1L, List.of(2L)))
                .thenReturn(List.of(item));
        LibraryService service = service();

        // when
        service.bulkChangeStatus(1L, List.of(2L), ReadingStatus.FINISHED);

        // then
        InOrder lockOrder = inOrder(libraryItemRepository, bookRepository);
        lockOrder.verify(libraryItemRepository).findAllByMemberIdAndBookIdIn(1L, List.of(2L));
        lockOrder.verify(bookRepository).findByIdForUpdate(2L);
        lockOrder.verify(libraryItemRepository).findAllByMemberIdAndBookIdInForUpdate(1L, List.of(2L));
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.FINISHED);
    }

    @Test
    @DisplayName("같은 별점을 준 책이 없다면 current를 null로 반환한다")
    void should_ReturnNullCurrent_When_NoBookHasCriterionRating() {
        // given
        Book targetBook = mock(Book.class);
        when(targetBook.getId()).thenReturn(10L);
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(java.util.Optional.of(targetBook));
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5")))
                .thenReturn(java.util.Optional.empty());
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5")))
                .thenReturn(java.util.Optional.empty());
        LibraryService service = service();

        // when
        var response = service.compareRatingsByIsbn13(1L, ISBN13, new BigDecimal("4.5"));

        // then
        assertThat(response.current()).isNull();
    }

    @Test
    @DisplayName("같은 별점을 준 책 중 최근 별점 도서를 current로 반환한다")
    void should_ReturnMostRecentlyRatedBookAsCurrent_When_BooksHaveCriterionRating() {
        // given
        Book targetBook = mock(Book.class);
        when(targetBook.getId()).thenReturn(10L);
        LibraryItem sameRatedItem = LibraryItem.create(1L, 9L, ReadingStatus.READING, null,
                CLOCK.instant());
        sameRatedItem.rate(new BigDecimal("4.5"), CLOCK.instant().plusSeconds(10));
        Book sameRatedBook = mock(Book.class);
        when(sameRatedBook.getIsbn13()).thenReturn(new Isbn13("9788925568683"));
        when(sameRatedBook.getTitle()).thenReturn("같은 별점 도서");
        when(sameRatedBook.getCoverImageUrl()).thenReturn("https://example.com/cover.jpg");
        when(sameRatedBook.getAuthors()).thenReturn(List.of("작가"));
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(java.util.Optional.of(targetBook));
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5")))
                .thenReturn(java.util.Optional.of(sameRatedItem));
        when(bookRepository.findById(9L)).thenReturn(java.util.Optional.of(sameRatedBook));
        LibraryService service = service();

        // when
        var response = service.compareRatingsByIsbn13(1L, ISBN13, new BigDecimal("4.5"));

        // then
        assertThat(response.current()).extracting("bookId", "myRating", "ratingUpdatedAt")
                .containsExactly(9L, new BigDecimal("4.5"), CLOCK.instant().plusSeconds(10));
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5"));
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5"));
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                        1L, 10L, new BigDecimal("4.5"));
    }

    @Test
    @DisplayName("댓글순 서재 조회는 감상·답글 집계가 많은 책부터 반환한다")
    void should_OrderByCommentCount_When_ListingLibraryWithCommentSort() {
        // given
        LibraryItem firstItem = LibraryItem.create(
                1L, 2L, ReadingStatus.WANT_TO_READ, null, CLOCK.instant());
        LibraryItem secondItem = LibraryItem.create(
                1L, 3L, ReadingStatus.WANT_TO_READ, null, CLOCK.instant());
        Book firstBook = mock(Book.class);
        Book secondBook = mock(Book.class);
        when(firstBook.getId()).thenReturn(2L);
        when(secondBook.getId()).thenReturn(3L);
        when(firstBook.getIsbn13()).thenReturn(new Isbn13("9788925568683"));
        when(secondBook.getIsbn13()).thenReturn(new Isbn13("9788936433598"));
        when(libraryItemRepository.findAllByMemberId(1L)).thenReturn(List.of(firstItem, secondItem));
        when(bookRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(firstBook, secondBook));
        when(commentCountReader.getCommentCounts(anyCollection()))
                .thenReturn(Map.of(2L, 1L, 3L, 5L));
        when(libraryItemRepository.countByMemberId(1L)).thenReturn(2L);
        LibraryService service = service();

        // when
        var response = service.getLibrary(1L, 1, null, LibrarySort.COMMENT);

        // then
        assertThat(response.items()).extracting("bookId", "commentCount")
                .containsExactly(tuple(3L, 5L), tuple(2L, 1L));
    }

    @Test
    @DisplayName("활성 회원의 빈 공개 서재는 빈 목록을 반환한다")
    void should_ReturnEmptyPublicLibrary_When_ActiveMemberHasNoItems() {
        Member member = mock(Member.class);
        when(member.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member));
        when(libraryItemRepository.findAllByMemberId(1L)).thenReturn(List.of());
        when(commentCountReader.getCommentCounts(java.util.Set.of())).thenReturn(Map.of());

        var response = service().getPublicLibrary(1L, 1, null, LibrarySort.RECENT);

        assertThat(response.totalCount()).isZero();
        assertThat(response.filteredCount()).isZero();
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않거나 비활성 상태인 회원의 공개 서재는 조회할 수 없다")
    void should_RejectPublicLibrary_When_MemberIsMissingOrInactive() {
        when(memberRepository.findById(1L)).thenReturn(java.util.Optional.empty());
        assertLibraryNotFound(1L);

        for (AccountStatus status : List.of(AccountStatus.WITHDRAWN, AccountStatus.SUSPENDED)) {
            Member member = mock(Member.class);
            when(member.getAccountStatus()).thenReturn(status);
            when(memberRepository.findById(1L)).thenReturn(java.util.Optional.of(member));
            assertLibraryNotFound(1L);
        }
    }

    private void assertLibraryNotFound(long memberId) {
        assertThatThrownBy(() -> service().getPublicLibrary(memberId, 1, null, LibrarySort.RECENT))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND));
    }

    private LibraryService service() {
        return new LibraryService(libraryItemRepository, bookRepository, bookResolver,
                commentCountReader, memberRepository, CLOCK, transactionManager);
    }

    private Book book(Integer totalPages) {
        return Book.create(new Isbn13("9788925568683"), "마션", "https://example.com/cover.jpg", null, List.of("앤디 위어"),
                List.of(), "알에이치코리아", "SF", null, totalPages);
    }
}
