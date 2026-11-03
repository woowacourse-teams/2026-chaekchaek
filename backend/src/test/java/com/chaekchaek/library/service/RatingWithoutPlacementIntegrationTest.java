package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:rating-only-library;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RatingWithoutPlacementIntegrationTest {

    private static final Instant ADDED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private LibraryService service;

    @Autowired
    private LibraryItemRepository records;

    @Autowired
    private BookRepository books;

    @Autowired
    private MemberRepository members;

    @Autowired
    private ReadingRecordCoordinator readingRecordCoordinator;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private LibraryRepository libraries;

    @MockitoSpyBean
    private LibraryBookRepository placements;

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
    @DisplayName("닉네임과 기본 책장 없이 별점만 저장하고 독서 필드와 배치를 만들지 않는다")
    void should_CreateRatingOnlyRecord_When_MemberHasNoNicknameOrLibrary() {
        // given
        Book book = book(null);
        clearInvocations(libraries);
        clearInvocations(placements);

        // when
        var response = service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));

        // then
        verifyNoInteractions(libraries, placements);
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(response.status()).isNull();
        assertThat(response.currentPage()).isNull();
        assertThat(response.readingUpdatedAt()).isNull();
        assertThat(item(book).getRatingUpdatedAt()).isNotNull();
        assertThat(item(book).getAddedAt()).isNotNull();
        assertThat(placements.count()).isZero();
        assertThat(books.findById(book.getId()).orElseThrow().getTotalPages()).isNull();
        assertThat(service.getLibrary(owner.getId(), 1, null, LibrarySort.RECENT).items()).isEmpty();
    }

    @Test
    @DisplayName("기본 책장에 이미 30권이 있어도 새 도서의 별점을 저장한다")
    void should_IgnoreLibraryCapacity_When_RatingNewBook() {
        // given
        Library main = library(true, 1L);
        for (int index = 1; index <= 30; index++) {
            Book book = book(100);
            records.saveAndFlush(LibraryItem.create(owner.getId(), book.getId(), ReadingStatus.READING, 100, ADDED_AT));
            placements.saveAndFlush(LibraryBook.create(main.getId(), book.getId(), index));
        }
        Book book = book(null);

        // when
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));

        // then
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(30L);
        assertThat(item(book).getStatus()).isNull();
        assertThat(item(book).getCurrentPage()).isNull();
        assertThat(service.getLibrary(owner.getId(), 1, null, LibrarySort.RECENT).totalCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("기존 독서 기록을 가진 도서의 별점 수정은 독서 상태와 페이지 및 시각을 보존한다")
    void should_PreserveReadingRecord_When_UpdatingRating() {
        // given
        Book book = book(100);
        LibraryItem record = LibraryItem.create(owner.getId(), book.getId(), ReadingStatus.READING, 100, ADDED_AT);
        record.changeCurrentPage(23, 100, ADDED_AT.plusSeconds(10));
        records.saveAndFlush(record);

        // when
        service.rate(owner.getId(), book.getId(), new BigDecimal("3.5"));

        // then
        LibraryItem actual = item(book);
        assertThat(actual.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(actual.getCurrentPage()).isEqualTo(23);
        assertThat(actual.getReadingUpdatedAt()).isEqualTo(ADDED_AT.plusSeconds(10));
        assertThat(actual.getAddedAt()).isEqualTo(ADDED_AT);
        assertThat(actual.getRating()).isEqualByComparingTo("3.5");
        assertThat(placements.count()).isZero();
    }

    @Test
    @DisplayName("마지막 배치 삭제로 초기화된 기록에 별점을 저장해도 독서 상태를 복원하지 않는다")
    void should_KeepClearedState_When_RatingAfterLastPlacementDeletion() {
        // given
        Library main = library(true, 1L);
        Book book = book(100);
        service.add(owner.getId(), book.getId(), ReadingStatus.READING, null);
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));
        long originalId = item(book).getId();
        Instant originalAddedAt = item(book).getAddedAt();
        service.delete(owner.getId(), book.getId());

        // when
        service.rate(owner.getId(), book.getId(), new BigDecimal("3.5"));

        // then
        assertThat(item(book).getStatus()).isNull();
        assertThat(item(book).getCurrentPage()).isNull();
        assertThat(item(book).getReadingUpdatedAt()).isNull();
        assertThat(item(book).getId()).isEqualTo(originalId);
        assertThat(item(book).getAddedAt()).isEqualTo(originalAddedAt);
        assertThat(item(book).getRating()).isEqualByComparingTo("3.5");
        assertThat(placements.countByLibraryId(main.getId())).isZero();
    }

    @Test
    @DisplayName("같은 별점을 다시 저장해도 공유행과 평가 수를 늘리지 않는다")
    void should_ReuseSharedRecord_When_RatingSameBookRepeatedly() {
        // given
        Book book = book(100);
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));
        long originalId = item(book).getId();
        Instant originalAddedAt = item(book).getAddedAt();

        // when
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));

        // then
        assertThat(records.count()).isEqualTo(1L);
        assertThat(item(book).getId()).isEqualTo(originalId);
        assertThat(item(book).getAddedAt()).isEqualTo(originalAddedAt);
        assertThat(records.findRatingStatisticsByBookIdIn(List.of(book.getId()))).singleElement()
                .satisfies(statistics -> assertThat(statistics.getRatingCount()).isEqualTo(1L));
    }

    @Test
    @DisplayName("여러 책장에 같은 도서가 있어도 회원별 별점을 한 번씩 집계한다")
    void should_CountEachMemberRatingOnce_When_BookHasMultiplePlacements() {
        // given
        Library main = library(true, 1L);
        Library other = library(false, 2L);
        Book book = book(100);
        service.add(owner.getId(), book.getId(), ReadingStatus.READING, null);
        placements.saveAndFlush(LibraryBook.create(other.getId(), book.getId(), 1L));
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));
        Member second = members.saveAndFlush(Member.create("다른회원", null, LocalDateTime.now()));

        // when
        service.rate(second.getId(), book.getId(), new BigDecimal("3.5"));

        // then
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(1L);
        assertThat(records.findRatingStatisticsByBookIdIn(List.of(book.getId()))).singleElement()
                .satisfies(statistics -> {
                    assertThat(statistics.getRatingCount()).isEqualTo(2L);
                    assertThat(statistics.getAverageRating()).isEqualTo(4.0);
                });
    }

    @Test
    @DisplayName("잘못된 별점과 없는 도서 오류는 기존 별점과 공유행을 바꾸지 않는다")
    void should_PreserveData_When_RatingRequestIsInvalid() {
        // given
        Book book = book(100);
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));

        // when & then
        assertThatThrownBy(() -> service.rate(owner.getId(), book.getId(), new BigDecimal("5.1")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThatThrownBy(() -> service.rate(owner.getId(), Long.MAX_VALUE, new BigDecimal("4.5")))
                .isInstanceOf(BookNotFoundException.class);
        assertThat(item(book).getRating()).isEqualByComparingTo("4.5");
        assertThat(records.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("별점 삭제는 별점 전용 공유행과 최초 시각을 보존한다")
    void should_PreserveEmptySharedRecord_When_RemovingOnlyRating() {
        // given
        Book book = book(100);
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));
        long originalId = item(book).getId();
        Instant originalAddedAt = item(book).getAddedAt();

        // when
        service.removeRating(owner.getId(), book.getId());

        // then
        assertThat(item(book).getId()).isEqualTo(originalId);
        assertThat(item(book).getAddedAt()).isEqualTo(originalAddedAt);
        assertThat(item(book).getStatus()).isNull();
        assertThat(item(book).getRating()).isNull();
        assertThat(item(book).getRatingUpdatedAt()).isNull();
        assertThat(placements.count()).isZero();
    }

    @Test
    @DisplayName("감상 연계는 별점 전용 공유행을 재사용해 독서를 시작하고 별점과 최초 시각을 유지한다")
    void should_ReuseRatingOnlyRecord_When_RecordingReview() {
        // given
        Book book = book(100);
        service.rate(owner.getId(), book.getId(), new BigDecimal("4.5"));
        long originalId = item(book).getId();
        Instant originalAddedAt = item(book).getAddedAt();

        // when
        new TransactionTemplate(transactionManager).executeWithoutResult(transactionStatus ->
                readingRecordCoordinator.recordReview(owner.getId(), book.getId(), 23, null));

        // then
        assertThat(item(book).getId()).isEqualTo(originalId);
        assertThat(item(book).getAddedAt()).isEqualTo(originalAddedAt);
        assertThat(item(book).getRating()).isEqualByComparingTo("4.5");
        assertThat(item(book).getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item(book).getCurrentPage()).isEqualTo(23);
        assertThat(item(book).getReadingUpdatedAt()).isNotNull();
        assertThat(placements.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"WITHDRAWN", "SUSPENDED"})
    @DisplayName("회원 잠금 직전에 비활성으로 변경되면 기존 서재와 별점 쓰기를 모두 거절한다")
    void should_RecheckActiveMemberUnderLock_When_StatusChangesBeforeWrite(AccountStatus accountStatus) {
        // given
        Library main = library(true, 1L);
        Book book = book(100);
        records.saveAndFlush(LibraryItem.create(owner.getId(), book.getId(), ReadingStatus.READING, 100, ADDED_AT));
        placements.saveAndFlush(LibraryBook.create(main.getId(), book.getId(), 1L));
        ReflectionTestUtils.setField(owner, "accountStatus", accountStatus);
        members.saveAndFlush(owner);
        List<Runnable> writes = List.of(
                () -> service.add(owner.getId(), book.getId(), ReadingStatus.READING, null),
                () -> service.update(owner.getId(), book.getId(), ReadingStatus.FINISHED, null, null),
                () -> service.delete(owner.getId(), book.getId()),
                () -> service.bulkDelete(owner.getId(), List.of(book.getId())),
                () -> service.bulkChangeStatus(owner.getId(), List.of(book.getId()), ReadingStatus.FINISHED),
                () -> service.rate(owner.getId(), book.getId(), new BigDecimal("4.5")),
                () -> service.removeRating(owner.getId(), book.getId())
        );

        // when & then
        for (Runnable write : writes) {
            assertThatThrownBy(write::run).isInstanceOfSatisfying(BusinessException.class,
                    exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        }
        assertThat(placements.countByLibraryId(main.getId())).isEqualTo(1L);
        assertThat(item(book).getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item(book).getRating()).isNull();
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

    private LibraryItem item(Book book) {
        return records.findByMemberIdAndBookId(owner.getId(), book.getId()).orElseThrow();
    }
}
