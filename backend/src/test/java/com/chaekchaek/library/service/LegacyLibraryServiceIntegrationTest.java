package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:legacy-library;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class LegacyLibraryServiceIntegrationTest {

    private static final Instant ADDED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private LibraryService service;

    @Autowired
    private LibraryRepository libraries;

    @Autowired
    private LibraryItemRepository records;

    @Autowired
    private BookRepository books;

    @Autowired
    private MemberRepository members;

    @MockitoSpyBean
    private LibraryBookRepository placements;

    @MockitoSpyBean
    private BookResolver resolver;

    private Member owner;
    private int bookSequence;

    @BeforeEach
    void createMember() {
        owner = members.saveAndFlush(Member.create("익명", null, LocalDateTime.now()));
    }

    @AfterEach
    void deleteFixtures() {
        placements.deleteAllInBatch();
        records.deleteAllInBatch();
        libraries.deleteAllInBatch();
        books.deleteAllInBatch();
        members.deleteAllInBatch();
    }

    @Test
    @DisplayName("기존 조회는 기본 책장 배치만 필터와 기존 정렬 및 페이지 개수에 반영한다")
    void should_ListOnlyDefaultPlacements_When_OtherRecordsAndLibrariesExist() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        List<Long> readingIds = new ArrayList<>();
        for (int index = 1; index <= 12; index++) {
            Book book = book(100);
            LibraryItem record = record(book, ReadingStatus.READING);
            record.changeCurrentPage(index, 100, ADDED_AT.plusSeconds(index));
            records.saveAndFlush(record);
            place(main, book, 13L - index);
            readingIds.addFirst(book.getId());
        }
        Book finished = book(100);
        record(finished, ReadingStatus.FINISHED);
        place(main, finished, 14L);
        Book onlyOther = book(100);
        record(onlyOther, ReadingStatus.READING);
        place(other, onlyOther, 1L);
        record(book(100), ReadingStatus.READING);

        // when
        LibraryListResponse first = service.getLibrary(owner.getId(), 1, ReadingStatus.READING, LibrarySort.RECENT);
        LibraryListResponse second = service.getLibrary(owner.getId(), 2, ReadingStatus.READING, LibrarySort.RECENT);
        LibraryListResponse maximum = service.getLibrary(owner.getId(), Integer.MAX_VALUE, null, LibrarySort.RECENT);

        // then
        assertThat(first.totalCount()).isEqualTo(13L);
        assertThat(first.filteredCount()).isEqualTo(12L);
        assertThat(first.items()).extracting(item -> item.bookId()).containsExactlyElementsOf(readingIds.subList(0, 10));
        assertThat(first.nextPage()).isEqualTo(2);
        assertThat(second.items()).extracting(item -> item.bookId()).containsExactlyElementsOf(readingIds.subList(10, 12));
        assertThat(second.nextPage()).isNull();
        assertThat(maximum.items()).isEmpty();
        assertThat(maximum.nextPage()).isNull();
    }

    @Test
    @DisplayName("기본 책장이 없으면 기록이 있어도 기존 조회는 비고 기본 책장 쓰기 계약을 구분한다")
    void should_HandleMissingDefaultLibrary_When_SharedRecordsExist() {
        // given
        Book book = book(100);
        record(book, ReadingStatus.READING);

        // when & then
        assertThat(service.getLibrary(owner.getId(), 1, null, LibrarySort.RECENT).items()).isEmpty();
        assertError(() -> service.add(owner.getId(), book.getId(), ReadingStatus.READING, null), ErrorCode.NICKNAME_REQUIRED);
        service.delete(owner.getId(), book.getId());
        assertThat(item(book).getStatus()).isEqualTo(ReadingStatus.READING);
        assertThatThrownBy(() -> service.delete(owner.getId(), Long.MAX_VALUE)).isInstanceOf(BookNotFoundException.class);
        assertError(() -> service.bulkDelete(owner.getId(), List.of(book.getId())), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 책장에 있는 도서를 기존 추가로 담으면 상태와 별점을 유지하고 기본 책장 맨 뒤에 배치한다")
    void should_AppendDefaultPlacementAndPreserveRecord_When_BookIsInAnotherLibrary() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book previous = book(null);
        record(previous, ReadingStatus.READING);
        place(main, previous, 8L);
        Book book = book(null);
        LibraryItem record = record(book, ReadingStatus.READING);
        record.rate(new BigDecimal("4.5"), ADDED_AT);
        records.saveAndFlush(record);
        place(other, book, 1L);

        // when
        service.add(owner.getId(), book.getId(), ReadingStatus.FINISHED, null);

        // then
        LibraryItem actual = item(book);
        assertThat(actual.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(actual.getCurrentPage()).isZero();
        assertThat(actual.getReadingUpdatedAt()).isEqualTo(ADDED_AT);
        assertThat(actual.getRating()).isEqualByComparingTo("4.5");
        assertThat(actual.getAddedAt()).isEqualTo(ADDED_AT);
        assertThat(placements.findByLibraryIdAndBookId(main.getId(), book.getId())).get()
                .extracting(LibraryBook::getDisplayOrder).isEqualTo(9L);
    }

    @Test
    @DisplayName("초기화된 기존 공유행은 요청 독서 상태로 다시 시작하고 별점과 최초 시각은 보존한다")
    void should_RestartClearedState_When_AddingBookToDefaultLibrary() {
        // given
        Library main = library(true, 1L);
        Book book = book(100);
        LibraryItem record = record(book, ReadingStatus.READING);
        record.rate(new BigDecimal("3.5"), ADDED_AT);
        record.clearReadingState();
        records.saveAndFlush(record);

        // when
        service.add(owner.getId(), book.getId(), ReadingStatus.FINISHED, null);

        // then
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(1L);
        assertThat(item(book).getStatus()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(item(book).getCurrentPage()).isEqualTo(100);
        assertThat(item(book).getRating()).isEqualByComparingTo("3.5");
        assertThat(item(book).getAddedAt()).isEqualTo(ADDED_AT);
    }

    @Test
    @DisplayName("기본 책장의 같은 도서 중복과 30권 한도를 기존 추가에서도 적용한다")
    void should_RejectDuplicateAndCapacity_When_AddingThroughLegacyApi() {
        // given
        Library main = library(true, 1L);
        Book existing = book(null);
        record(existing, ReadingStatus.READING);
        place(main, existing, 1L);
        for (int index = 2; index <= 30; index++) {
            Book book = book(100);
            record(book, ReadingStatus.READING);
            place(main, book, index);
        }
        Book overflow = book(null);

        // when & then
        assertError(() -> service.add(owner.getId(), existing.getId(), ReadingStatus.READING, 200), ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        assertError(() -> service.add(owner.getId(), overflow.getId(), ReadingStatus.READING, 200), ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED);
        assertThat(books.findById(existing.getId()).orElseThrow().getTotalPages()).isNull();
        assertThat(books.findById(overflow.getId()).orElseThrow().getTotalPages()).isNull();
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(30L);
        assertThat(records.findByMemberIdAndBookId(owner.getId(), overflow.getId())).isEmpty();
    }

    @Test
    @DisplayName("기본 책장의 마지막 배치 삭제는 공유행과 별점을 남기고 독서 기록만 초기화한다")
    void should_ClearReadingStateOnly_When_RemovingLastDefaultPlacement() {
        // given
        Library main = library(true, 1L);
        Book book = book(100);
        LibraryItem record = record(book, ReadingStatus.READING);
        record.rate(new BigDecimal("4.5"), ADDED_AT);
        records.saveAndFlush(record);
        place(main, book, 1L);

        // when
        service.delete(owner.getId(), book.getId());

        // then
        assertThat(placements.countByLibraryId(main.getId())).isZero();
        assertThat(item(book).getStatus()).isNull();
        assertThat(item(book).getCurrentPage()).isNull();
        assertThat(item(book).getReadingUpdatedAt()).isNull();
        assertThat(item(book).getRating()).isEqualByComparingTo("4.5");
        assertThat(item(book).getRatingUpdatedAt()).isEqualTo(ADDED_AT);
        assertThat(item(book).getAddedAt()).isEqualTo(ADDED_AT);
        assertThat(item(book).getId()).isEqualTo(record.getId());
    }

    @Test
    @DisplayName("기본 배치가 없거나 다른 책장 배치가 남으면 기존 삭제로 독서 기록을 초기화하지 않는다")
    void should_PreserveState_When_AnotherPlacementRemainsOrDefaultPlacementIsMissing() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book book = book(100);
        record(book, ReadingStatus.READING);
        place(main, book, 1L);
        place(other, book, 1L);

        // when
        service.delete(owner.getId(), book.getId());
        service.delete(owner.getId(), book.getId());

        // then
        assertThat(placements.countByLibraryId(main.getId())).isZero();
        assertThat(placements.countByLibraryId(other.getId())).isEqualTo(1L);
        assertThat(item(book).getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("일괄 삭제 대상이 하나라도 기본 책장에 없으면 기존 배치와 공유 기록을 모두 유지한다")
    void should_RejectWholeBulkDeletion_When_OneDefaultPlacementIsMissing() {
        // given
        Library main = library(true, 1L);
        Book placed = book(100);
        Book missing = book(100);
        record(placed, ReadingStatus.READING);
        record(missing, ReadingStatus.READING);
        place(main, placed, 1L);

        // when & then
        assertError(() -> service.bulkDelete(owner.getId(), List.of(placed.getId(), missing.getId())), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(1L);
        assertThat(item(placed).getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("일괄 삭제는 남은 배치를 일괄 조회하고 도서마다 마지막 배치인지 판단한다")
    void should_BatchCheckRemainingPlacements_When_DeletingMultipleBooks() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book last = book(100);
        Book remaining = book(100);
        record(last, ReadingStatus.READING);
        record(remaining, ReadingStatus.READING);
        place(main, last, 1L);
        place(main, remaining, 2L);
        place(other, remaining, 1L);

        // when
        service.bulkDelete(owner.getId(), List.of(last.getId(), remaining.getId()));

        // then
        assertThat(placements.countByLibraryId(main.getId())).isZero();
        assertThat(item(last).getStatus()).isNull();
        assertThat(item(remaining).getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(records.count()).isEqualTo(2L);
        verify(placements, never()).countByMemberIdAndBookId(owner.getId(), last.getId());
        verify(placements, never()).countByMemberIdAndBookId(owner.getId(), remaining.getId());
    }

    @Test
    @DisplayName("초기화된 독서 기록의 단건과 일괄 수정은 다른 기록을 바꾸지 않고 거절한다")
    void should_RejectClearedReadingRecords_When_UpdatingSingleOrBulkState() {
        // given
        Book active = book(100);
        Book cleared = book(100);
        record(active, ReadingStatus.READING);
        LibraryItem record = record(cleared, ReadingStatus.READING);
        record.clearReadingState();
        records.saveAndFlush(record);

        // when & then
        assertError(() -> service.update(owner.getId(), cleared.getId(), ReadingStatus.FINISHED, null, null), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        assertError(() -> service.bulkChangeStatus(owner.getId(), List.of(active.getId(), cleared.getId()), ReadingStatus.FINISHED), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        assertThat(item(active).getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item(cleared).getStatus()).isNull();
    }

    @Test
    @DisplayName("ISBN 확보를 회원 잠금과 쓰기 트랜잭션 밖에서 실행한다")
    void should_ResolveIsbnOutsideTransaction_When_AddingThroughLegacyApi() {
        // given
        library(true, 1L);
        Book book = book(100);
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return book;
        }).when(resolver).findOrCreate(book.getIsbn13());

        // when
        service.addByIsbn13(owner.getId(), book.getIsbn13(), ReadingStatus.READING, null);

        // then
        assertThat(placements.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("배치 저장이 실패하면 메타데이터와 독서 재시작도 실제 트랜잭션에서 되돌린다")
    void should_RollBackMetadataAndRecord_When_PlacementPersistenceFails() {
        // given
        library(true, 1L);
        Book book = book(null);
        LibraryItem record = record(book, ReadingStatus.READING);
        record.rate(new BigDecimal("4.5"), ADDED_AT);
        record.clearReadingState();
        records.saveAndFlush(record);
        doThrow(new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS)).when(placements).create(any());

        // when & then
        assertError(() -> service.add(owner.getId(), book.getId(), ReadingStatus.FINISHED, 100), ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        assertThat(books.findById(book.getId()).orElseThrow().getTotalPages()).isNull();
        assertThat(item(book).getStatus()).isNull();
        assertThat(item(book).getRating()).isEqualByComparingTo("4.5");
        assertThat(placements.count()).isZero();
    }

    private Library library(boolean isDefault, long order) {
        Library library = Library.create(owner.getId(), "책장" + order, ADDED_AT);
        library.changeDisplayOrder(order);
        ReflectionTestUtils.setField(library, "isDefault", isDefault);
        return libraries.saveAndFlush(library);
    }

    private Book book(Integer totalPages) {
        return books.saveAndFlush(Book.create(
                new Isbn13(String.format("978%010d", ++bookSequence)),
                "책" + bookSequence,
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("저자"),
                List.of(),
                "출판사",
                "분류",
                null,
                totalPages,
                null,
                null
        ));
    }

    private LibraryItem record(Book book, ReadingStatus status) {
        return records.saveAndFlush(LibraryItem.create(owner.getId(), book.getId(), status, book.getTotalPages(), ADDED_AT));
    }

    private LibraryItem item(Book book) {
        return records.findByMemberIdAndBookId(owner.getId(), book.getId()).orElseThrow();
    }

    private void place(Library library, Book book, long order) {
        placements.saveAndFlush(LibraryBook.create(library.getId(), book.getId(), order));
    }

    private void assertError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode)
        );
    }
}
