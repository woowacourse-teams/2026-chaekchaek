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
    private LibraryQueryRepository queryRepository;

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

    @Test
    @DisplayName("35권 이전 책장을 전체 조회하고 고정 크기의 첫 페이지와 마지막 페이지로 조회한다")
    void should_ReadAllMigratedBooksAndPages_When_LibraryExceedsThirtyBooks() {
        // given
        Member owner = member();
        Library library = library(owner, "이전 책장", 1L);
        List<Long> bookIds = new ArrayList<>();
        for (int index = 1; index <= 35; index++) {
            Book book = book(index);
            record(owner, book, ReadingStatus.READING);
            placement(library, book, index);
            bookIds.add(book.getId());
        }
        flushAndClear();

        // when
        LibraryCollectionResponse collection = service.findLibraries(owner.getId());
        LibraryDetailResponse first = service.findLibrary(library.getId(), 1);
        LibraryDetailResponse last = service.findLibrary(library.getId(), 4);
        LibraryDetailResponse beyondLast = service.findLibrary(library.getId(), 5);
        LibraryDetailResponse maximumPage = service.findLibrary(library.getId(), Integer.MAX_VALUE);

        // then
        assertThat(collection.libraries().getFirst().totalCount()).isEqualTo(35L);
        assertThat(collection.libraries().getFirst().items()).hasSize(35);
        assertThat(first.totalCount()).isEqualTo(35L);
        assertThat(first.items()).extracting(item -> item.bookId()).containsExactlyElementsOf(bookIds.subList(0, 10));
        assertThat(first.nextPage()).isEqualTo(2);
        assertThat(last.items()).extracting(item -> item.bookId()).containsExactlyElementsOf(bookIds.subList(30, 35));
        assertThat(last.nextPage()).isNull();
        assertThat(beyondLast.totalCount()).isEqualTo(35L);
        assertThat(beyondLast.items()).isEmpty();
        assertThat(beyondLast.nextPage()).isNull();
        assertThat(maximumPage.items()).isEmpty();
        assertThat(maximumPage.nextPage()).isNull();
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
        LibraryDetailResponse response = service.findLibrary(library.getId(), 1);

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
        verify(jdbcTemplate).queryForObject("select count(*) from library_book where library_id = ?", Long.class, library.getId());
        verify(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
    }

    @Test
    @DisplayName("빈 서재와 빈 책장은 빈 목록과 null 다음 페이지를 반환한다")
    void should_ReturnEmptyItems_When_MemberOrShelfHasNoPlacements() {
        // given
        Member owner = member();
        flushAndClear();

        // when & then
        assertThat(service.findLibraries(owner.getId()).libraries()).isEmpty();
        Library library = library(owner, "빈 책장", 1L);
        flushAndClear();
        LibraryDetailResponse response = service.findLibrary(library.getId(), 1);
        assertThat(response.totalCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.nextPage()).isNull();
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
        assertThatThrownBy(() -> service.findLibrary(library.getId(), 1))
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
        assertThatThrownBy(() -> service.findLibrary(missingId, 1))
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
        assertThatThrownBy(() -> service.findLibrary(library.getId(), 1)).isInstanceOf(IllegalStateException.class);
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
        assertThatThrownBy(() -> service.findLibrary(library.getId(), 1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("페이지 조회는 도서마다 추가 조회하지 않고 큰 offset도 정수 오버플로 없이 처리한다")
    void should_ReadPageWithFixedQueries_When_OffsetExceedsIntegerRange() {
        // given
        Member owner = member();
        Library library = library(owner, "빈 책장", 1L);
        flushAndClear();
        clearInvocations(jdbcTemplate);

        // when
        List<?> response = queryRepository.findBooksByLibraryId(library.getId(), 21_474_836_460L, 10);

        // then
        assertThat(response).isEmpty();
        verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Object>>any(), any(Object[].class));
    }

    @Test
    @DisplayName("상세 저자는 해당 페이지 도서만 한 번 조회해 저장 순서를 유지하고 없으면 빈 배열로 반환한다")
    void should_BatchLoadOrderedAuthorsForPageOnly_When_ReadingLibraryDetail() {
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
        LibraryDetailResponse lastPage = service.findLibrary(library.getId(), 2);

        // then
        assertThat(lastPage.items()).singleElement().satisfies(item -> {
            assertThat(item.bookId()).isEqualTo(lastBook.getId());
            assertThat(item.coverImageUrl()).isEqualTo("https://example.com/cover.jpg");
            assertThat(item.authors()).containsExactly("두 번째 저자", "첫 번째 저자");
        });
        ArgumentCaptor<Object[]> authorBookIds = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), authorBookIds.capture());
        assertThat(authorBookIds.getValue()).containsExactly(lastBook.getId());
        LibraryDetailResponse firstPage = service.findLibrary(library.getId(), 1);
        assertThat(firstPage.items()).hasSize(10).allSatisfy(item -> assertThat(item.authors()).isEmpty());
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
