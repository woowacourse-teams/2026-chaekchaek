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
    private LibraryBookService bookService;

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
    @DisplayName("기본 책장이 없으면 기록이 있어도 기존 조회는 비고 기본 책장 쓰기 계약을 구분한다")
    void should_HandleMissingDefaultLibrary_When_SharedRecordsExist() {
        // given
        Book book = book();
        record(book);

        // when & then
        assertThat(service.getLibrary(owner.getId(), 1, LibrarySort.RECENT).items()).isEmpty();
        assertError(() -> service.add(owner.getId(), book.getId()), ErrorCode.NICKNAME_REQUIRED);
        service.delete(owner.getId(), book.getId());
        assertThatThrownBy(() -> service.delete(owner.getId(), Long.MAX_VALUE)).isInstanceOf(BookNotFoundException.class);
        assertError(() -> service.bulkDelete(owner.getId(), List.of(book.getId())), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 책장에 있는 도서를 기존 추가로 담으면 상태와 별점을 유지하고 기본 책장 맨 뒤에 배치한다")
    void should_AppendDefaultPlacementAndPreserveRecord_When_BookIsInAnotherLibrary() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book previous = book();
        record(previous);
        place(main, previous, 8L);
        Book book = book();
        LibraryItem record = record(book);
        record.rate(new BigDecimal("4.5"), ADDED_AT);
        records.saveAndFlush(record);
        place(other, book, 1L);

        // when
        service.add(owner.getId(), book.getId());

        // then
        LibraryItem actual = item(book);
        assertThat(actual.getRating()).isEqualByComparingTo("4.5");
        assertThat(actual.getAddedAt()).isEqualTo(ADDED_AT);
        assertThat(placements.findByLibraryIdAndBookId(main.getId(), book.getId())).get()
                .extracting(LibraryBook::getDisplayOrder).isEqualTo(9L);
    }


    @Test
    @DisplayName("기본 책장의 같은 도서 중복과 30권 한도를 기존 추가에서도 적용한다")
    void should_RejectDuplicateAndCapacity_When_AddingThroughLegacyApi() {
        // given
        Library main = library(true, 1L);
        Book existing = book();
        record(existing);
        place(main, existing, 1L);
        for (int index = 2; index <= 30; index++) {
            Book book = book();
            record(book);
            place(main, book, index);
        }
        Book overflow = book();

        // when & then
        assertError(() -> service.add(owner.getId(), existing.getId()), ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        assertError(() -> service.add(owner.getId(), overflow.getId()), ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED);
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(30L);
        assertThat(records.findByMemberIdAndBookId(owner.getId(), overflow.getId())).isEmpty();
    }

    @Test
    @DisplayName("기본 책장의 마지막 배치를 삭제해도 공유 별점은 유지한다")
    void should_PreserveSharedRating_When_RemovingLastDefaultPlacement() {
        // given
        Library main = library(true, 1L);
        Book book = book();
        LibraryItem record = record(book);
        record.rate(new BigDecimal("4.5"), ADDED_AT);
        records.saveAndFlush(record);
        place(main, book, 1L);

        // when
        service.delete(owner.getId(), book.getId());

        // then
        assertThat(placements.countByLibraryId(main.getId())).isZero();
        assertThat(item(book).getRating()).isEqualByComparingTo("4.5");
        assertThat(item(book).getRatingUpdatedAt()).isEqualTo(ADDED_AT);
        assertThat(item(book).getAddedAt()).isEqualTo(ADDED_AT);
        assertThat(item(book).getId()).isEqualTo(record.getId());
    }


    @Test
    @DisplayName("일괄 삭제 대상이 하나라도 기본 책장에 없으면 기존 배치와 공유 기록을 모두 유지한다")
    void should_RejectWholeBulkDeletion_When_OneDefaultPlacementIsMissing() {
        // given
        Library main = library(true, 1L);
        Book placed = book();
        Book missing = book();
        record(placed);
        record(missing);
        place(main, placed, 1L);

        // when & then
        assertError(() -> service.bulkDelete(owner.getId(), List.of(placed.getId(), missing.getId())), ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(1L);
    }



    @Test
    @DisplayName("ISBN 확보를 회원 잠금과 쓰기 트랜잭션 밖에서 실행한다")
    void should_ResolveIsbnOutsideTransaction_When_AddingThroughLegacyApi() {
        // given
        library(true, 1L);
        Book book = book();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return book;
        }).when(resolver).findOrCreate(book.getIsbn13());

        // when
        service.addByIsbn13(owner.getId(), book.getIsbn13());

        // then
        assertThat(placements.count()).isEqualTo(1L);
    }


    @Test
    @DisplayName("기존 기본 책장 추가와 새 책장 삭제가 같은 공유 기록 생명주기를 따른다")
    void should_ShareLifecycle_When_LegacyAndNewPlacementApisAreMixed() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book book = book();
        record(book);
        place(other, book, 1L);
        service.add(owner.getId(), book.getId());

        // when
        bookService.delete(owner.getId(), other.getId(), book.getId());
        service.delete(owner.getId(), book.getId());

        // then
        assertThat(placements.countByLibraryId(main.getId())).isZero();
    }

    private Library library(boolean isDefault, long order) {
        Library library = Library.create(owner.getId(), "책장" + order, ADDED_AT);
        library.changeDisplayOrder(order);
        ReflectionTestUtils.setField(library, "isDefault", isDefault);
        return libraries.saveAndFlush(library);
    }

    private Book book() {
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
                null,
                null
        ));
    }

    private LibraryItem record(Book book) {
        return records.saveAndFlush(LibraryItem.create(owner.getId(), book.getId(), ADDED_AT));
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
