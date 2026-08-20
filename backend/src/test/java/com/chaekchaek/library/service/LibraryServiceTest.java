package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
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

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookResolver bookResolver;

    @Mock
    private BookCommentCountReader commentCountReader;

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

    private LibraryService service() {
        return new LibraryService(libraryItemRepository, bookRepository, bookResolver,
                commentCountReader, CLOCK, transactionManager);
    }

    private Book book(Integer totalPages) {
        return Book.create("9788925568683", "마션", "https://example.com/cover.jpg", null, List.of("앤디 위어"),
                List.of(), "알에이치코리아", "SF", null, totalPages);
    }
}
