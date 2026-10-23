package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryCollectionResponse;
import com.chaekchaek.library.dto.LibraryDetailResponse;
import com.chaekchaek.library.service.LibraryQueryService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@DataJpaTest
@ActiveProfiles("test")
@Import({LibraryQueryRepository.class, LibraryQueryService.class})
class LibraryQueryRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");

    @Autowired
    private LibraryQueryService service;

    @Autowired
    private MemberRepository members;

    @Autowired
    private LibraryRepository libraries;

    @Autowired
    private LibraryBookRepository placements;

    @Autowired
    private LibraryItemRepository records;

    @Autowired
    private BookRepository books;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("전체 조회는 책장과 도서 순서를 유지하고 빈 책장과 중복 도서 배치를 포함한다")
    void should_ReturnAllShelvesAndPlacements_When_FindingCollection() {
        // given
        Member owner = member();
        Library last = library(owner, "마지막", 3L);
        Library first = library(owner, "기존의 아주 긴 책장 이름 !", 1L);
        Library empty = library(owner, "빈 책장", 2L);
        Book firstBook = book(1);
        Book lastBook = book(2);
        record(owner, firstBook, ReadingStatus.READING);
        record(owner, lastBook, ReadingStatus.FINISHED);
        placement(first, lastBook, 2L);
        placement(first, firstBook, 1L);
        placement(last, firstBook, 1L);
        flushAndClear();
        jdbcTemplate.update("update library set is_default = true where id = ?", first.getId());
        clearInvocations(jdbcTemplate);

        // when
        LibraryCollectionResponse response = service.findLibraries(owner.getId());

        // then
        assertThat(response.libraries()).extracting(shelf -> shelf.libraryId())
                .containsExactly(first.getId(), empty.getId(), last.getId());
        assertThat(response.libraries().getFirst().name()).isEqualTo("기존의 아주 긴 책장 이름 !");
        assertThat(response.libraries().getFirst().isDefault()).isTrue();
        assertThat(response.libraries().getFirst().items()).extracting(item -> item.bookId())
                .containsExactly(firstBook.getId(), lastBook.getId());
        assertThat(response.libraries().getFirst().items().getFirst().spineImageUrl()).isNull();
        assertThat(response.libraries().get(1).items()).isEmpty();
        assertThat(response.libraries().getLast().items()).extracting(item -> item.bookId())
                .containsExactly(firstBook.getId());
        verify(jdbcTemplate, times(2)).query(anyString(), ArgumentMatchers.<RowMapper<Object>>any(), any(Object[].class));
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 35})
    @DisplayName("30권 책장과 한도를 초과한 기존 책장도 전체 도서를 순서대로 반환한다")
    void should_ReturnAllBooks_When_LibraryContainsThirtyOrMoreBooks(int count) {
        // given
        Member owner = member();
        Library library = library(owner, "이전 책장", 1L);
        List<Long> bookIds = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            Book book = book(index);
            record(owner, book, ReadingStatus.READING);
            placement(library, book, index);
            bookIds.add(book.getId());
        }
        flushAndClear();

        // when
        LibraryCollectionResponse collection = service.findLibraries(owner.getId());
        LibraryDetailResponse detail = service.findLibrary(library.getId());

        // then
        assertThat(collection.libraries().getFirst().totalCount()).isEqualTo(count);
        assertThat(collection.libraries().getFirst().items()).hasSize(count);
        assertThat(detail.totalCount()).isEqualTo(count);
        assertThat(detail.items()).extracting(item -> item.bookId()).containsExactlyElementsOf(bookIds);
    }

    @Test
    @DisplayName("책장 상세는 다른 회원 기록 대신 소유자의 독서 기록을 저장된 배치 순서로 반환한다")
    void should_ReadOwnerState_When_OtherMemberHasSameBook() {
        // given
        Member owner = member();
        Member other = member();
        Library library = library(owner, "소유자 책장", 1L);
        Book book = book(1);
        record(other, book, ReadingStatus.FINISHED);
        LibraryItem ownerRecord = record(owner, book, ReadingStatus.READING);
        ownerRecord.changeCurrentPage(23, book.getTotalPages(), NOW.plusSeconds(10));
        placement(library, book, 1L);
        flushAndClear();
        clearInvocations(jdbcTemplate);

        // when
        LibraryDetailResponse response = service.findLibrary(library.getId());

        // then
        assertThat(response.memberId()).isEqualTo(owner.getId());
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.status()).isEqualTo(ReadingStatus.READING);
            assertThat(item.currentPage()).isEqualTo(23);
            assertThat(item.readingUpdatedAt()).isEqualTo(NOW.plusSeconds(10));
            assertThat(item.totalPages()).isEqualTo(368);
            assertThat(item.coverImageUrl()).isEqualTo("https://example.com/cover.jpg");
            assertThat(item.authors()).containsExactly("저자");
            assertThat(item.isbn13()).isEqualTo(book.getIsbn13().value());
        });
        verify(jdbcTemplate, times(2)).query(anyString(), ArgumentMatchers.<RowMapper<Object>>any(), any(Object[].class));
        verify(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
    }

    @Test
    @DisplayName("빈 서재와 빈 책장은 빈 목록을 반환한다")
    void should_ReturnEmptyItems_When_MemberOrShelfHasNoPlacements() {
        // given
        Member owner = member();
        flushAndClear();

        // when & then
        assertThat(service.findLibraries(owner.getId()).libraries()).isEmpty();
        Library library = library(owner, "빈 책장", 1L);
        flushAndClear();
        LibraryDetailResponse response = service.findLibrary(library.getId());
        assertThat(response.totalCount()).isZero();
        assertThat(response.items()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"WITHDRAWN", "SUSPENDED"})
    @DisplayName("비활성 회원의 새 서재와 책장은 공개하지 않는다")
    void should_RejectPublicQueries_When_MemberIsInactive(String accountStatus) {
        // given
        Member owner = member();
        Library library = library(owner, "비공개", 1L);
        flushAndClear();
        jdbcTemplate.update("update member set account_status = ? where member_id = ?", accountStatus, owner.getId());

        // when & then
        assertThatThrownBy(() -> service.findLibraries(owner.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        assertThatThrownBy(() -> service.findLibrary(library.getId()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND));
    }

    @Test
    @DisplayName("없는 회원과 책장은 각 조회 계약의 오류로 구분한다")
    void should_ReturnDistinctNotFoundCodes_When_TargetIsMissing() {
        // given
        long missingId = Long.MAX_VALUE;

        // when & then
        assertThatThrownBy(() -> service.findLibraries(missingId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        assertThatThrownBy(() -> service.findLibrary(missingId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND));
    }

    @Test
    @DisplayName("공유 기록이 없는 배치를 전체 조회와 상세 조회에서 조용히 누락하지 않는다")
    void should_FailInvariant_When_PlacedBookHasNoSharedRecord() {
        // given
        Member owner = member();
        Library library = library(owner, "배치만 존재", 1L);
        Book book = book(1);
        placement(library, book, 1L);
        flushAndClear();

        // when & then
        assertThatThrownBy(() -> service.findLibraries(owner.getId())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.findLibrary(library.getId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("초기화된 공유 기록에 남은 배치를 전체 조회와 상세 조회에서 거부한다")
    void should_FailInvariant_When_PlacedBookHasClearedReadingState() {
        // given
        Member owner = member();
        Library library = library(owner, "잘못된 배치", 1L);
        Book book = book(1);
        LibraryItem record = record(owner, book, ReadingStatus.READING);
        placement(library, book, 1L);
        record.clearReadingState();
        flushAndClear();

        // when & then
        assertThatThrownBy(() -> service.findLibraries(owner.getId())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.findLibrary(library.getId())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("상세 저자는 책장 전체 도서를 한 번에 조회해 저장 순서를 유지하고 없으면 빈 배열로 반환한다")
    void should_BatchLoadOrderedAuthors_When_ReadingAllLibraryBooks() {
        // given
        Member owner = member();
        Library library = library(owner, "책장", 1L);
        for (int index = 1; index <= 10; index++) {
            Book book = book(index, List.of());
            record(owner, book, ReadingStatus.READING);
            placement(library, book, index);
        }
        Book lastBook = book(11, List.of("두 번째 저자", "첫 번째 저자"));
        record(owner, lastBook, ReadingStatus.READING);
        placement(library, lastBook, 11L);
        flushAndClear();
        clearInvocations(jdbcTemplate);

        // when
        LibraryDetailResponse detail = service.findLibrary(library.getId());

        // then
        assertThat(detail.items()).hasSize(11);
        assertThat(detail.items().getLast()).satisfies(item -> {
            assertThat(item.bookId()).isEqualTo(lastBook.getId());
            assertThat(item.coverImageUrl()).isEqualTo("https://example.com/cover.jpg");
            assertThat(item.authors()).containsExactly("두 번째 저자", "첫 번째 저자");
        });
        ArgumentCaptor<Object[]> authorBookIds = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), authorBookIds.capture());
        assertThat(authorBookIds.getValue()).containsExactlyElementsOf(detail.items()
                .stream()
                .map(item -> item.bookId())
                .toList());
        assertThat(detail.items().subList(0, 10)).allSatisfy(item -> assertThat(item.authors()).isEmpty());
    }

    private Member member() {
        return members.save(Member.create("익명", null, LocalDateTime.of(2026, 10, 23, 12, 0)));
    }

    private Library library(Member owner, String name, long displayOrder) {
        Library library = Library.create(owner.getId(), "책장" + displayOrder, NOW);
        library.changeDisplayOrder(displayOrder);
        libraries.saveAndFlush(library);
        jdbcTemplate.update("update library set name = ? where id = ?", name, library.getId());
        return library;
    }

    private Book book(int index) {
        return book(index, List.of("저자"));
    }

    private Book book(int index, List<String> authors) {
        return books.save(Book.create(
                new Isbn13(String.format("978%010d", index)),
                "도서 " + index,
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                authors,
                List.of(),
                "출판사",
                "분류",
                null,
                368,
                null,
                null
        ));
    }

    private LibraryItem record(Member owner, Book book, ReadingStatus status) {
        return records.save(LibraryItem.create(owner.getId(), book.getId(), status, book.getTotalPages(), NOW));
    }

    private void placement(Library library, Book book, long displayOrder) {
        placements.save(LibraryBook.create(library.getId(), book.getId(), displayOrder));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
