package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"),
            ZoneOffset.UTC
    );
    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");

    private final Logger logger = (Logger) LoggerFactory.getLogger(LibraryService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private LibraryBookRepository libraryBookRepository;

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

    @BeforeEach
    void setUp() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "5.1", "4.55"})
    @DisplayName("ISBN 별점의 값이 잘못되면 회원과 도서를 조회하지 않는다")
    void should_RejectBeforeLookup_When_IsbnRatingIsInvalid(String value) {
        // given
        BigDecimal rating = value == null ? null : new BigDecimal(value);

        // when & then
        assertThatThrownBy(() -> service().rateByIsbn13(1L, ISBN13, rating))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        verifyNoInteractions(memberRepository, bookResolver, transactionManager);
    }

    @Test
    @DisplayName("닉네임이 있는 활성 회원의 맨 뒤에 빈 일반 책장을 생성한다")
    void should_ReturnSavedLibrary_When_Created() {
        // given
        Library saved = mock(Library.class);
        when(saved.getId()).thenReturn(7L);
        when(saved.getName()).thenReturn("올해읽을책");
        when(saved.isDefault()).thenReturn(false);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.findMaxDisplayOrderByMemberId(1L)).thenReturn(4L);
        when(libraryRepository.create(any(Library.class))).thenReturn(saved);
        LibraryService service = service();

        // when
        LibraryResponse actual = service.create(1L, "올해읽을책");

        // then
        ArgumentCaptor<Library> captor = ArgumentCaptor.forClass(Library.class);
        InOrder lockOrder = inOrder(memberRepository, libraryRepository);
        lockOrder.verify(memberRepository).findByIdForUpdate(1L);
        lockOrder.verify(libraryRepository).findMaxDisplayOrderByMemberId(1L);
        lockOrder.verify(libraryRepository).create(captor.capture());
        Library created = captor.getValue();
        assertThat(created.isOwnedBy(1L)).isTrue();
        assertThat(created.getName()).isEqualTo("올해읽을책");
        assertThat(created.isDefault()).isFalse();
        assertThat(created.getDisplayOrder()).isEqualTo(5L);
        assertThat(created.getCreatedAt()).isEqualTo(CLOCK.instant());
        assertThat(actual.libraryId()).isEqualTo(7L);
        assertThat(actual.name()).isEqualTo("올해읽을책");
        assertThat(actual.isDefault()).isFalse();
        assertThat(actual.totalCount()).isZero();
        assertThat(actual.items()).isEmpty();
        assertThat(appender.list).singleElement()
                .satisfies(event -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .containsExactlyInAnyOrder(
                                tuple("type", "event"),
                                tuple("event", "library_created"),
                                tuple("memberId", 1L),
                                tuple("libraryId", 7L)
                        ));
    }

    @Test
    @DisplayName("도메인이 허용하지 않는 서재 이름이면 저장하지 않는다")
    void should_NotSaveLibrary_When_NameIsInvalid() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.create(1L, "   "))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
        verifyNoInteractions(libraryRepository);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("닉네임이 없으면 책장을 조회하거나 저장하지 않는다")
    void should_RequireNickname_BeforeCreatingLibrary() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithoutNickname()));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.create(1L, "올해읽을책"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NICKNAME_REQUIRED)
                );
        verifyNoInteractions(libraryRepository);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("비활성 회원은 책장을 조회하거나 저장할 수 없다")
    void should_RejectCreatingLibrary_When_MemberIsInactive() {
        // given
        Member inactiveMember = memberWithoutNickname();
        inactiveMember.withdraw(LocalDateTime.now());
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactiveMember));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.create(1L, "올해읽을책"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(libraryRepository);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("서재 저장이 실패하면 생성 이벤트를 기록하지 않는다")
    void should_NotLogCreatedEvent_When_PersistenceFails() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.create(any(Library.class)))
                .thenThrow(new BusinessException(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.create(1L, "올해읽을책"))
                .isInstanceOf(BusinessException.class);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("책장 이름을 변경하고 기본 여부와 순서를 보존한 이벤트를 기록한다")
    void should_RenameOwnedLibraryAndLogEvent_When_NameChanges() {
        // given
        Library library = Library.createDefault(1L, "올해읽을책", 5L, CLOCK.instant());
        ReflectionTestUtils.setField(library, "id", 7L);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.findById(7L)).thenReturn(Optional.of(library));
        LibraryService service = service();

        // when
        service.rename(1L, 7L, "읽을책");

        // then
        InOrder lockOrder = inOrder(memberRepository, libraryRepository);
        lockOrder.verify(memberRepository).findByIdForUpdate(1L);
        lockOrder.verify(libraryRepository).findById(7L);
        lockOrder.verify(libraryRepository).rename(library);
        assertThat(library.getName()).isEqualTo("읽을책");
        assertThat(library.isDefault()).isTrue();
        assertThat(library.getDisplayOrder()).isEqualTo(5L);
        assertThat(library.getUpdatedAt()).isEqualTo(CLOCK.instant());
        assertThat(appender.list).singleElement()
                .satisfies(event -> assertThat(event.getKeyValuePairs())
                        .extracting(pair -> pair.key, pair -> pair.value)
                        .containsExactlyInAnyOrder(
                                tuple("type", "event"),
                                tuple("event", "library_renamed"),
                                tuple("memberId", 1L),
                                tuple("libraryId", 7L)
                        ));
        assertThat(appender.list.getFirst().getKeyValuePairs())
                .extracting(pair -> pair.key)
                .doesNotContain("name");
    }

    @Test
    @DisplayName("현재 이름을 다시 요청하면 저장과 이벤트 없이 성공한다")
    void should_NotPersistOrLog_When_RenamingToCurrentName() {
        // given
        Library library = Library.create(1L, "올해읽을책", CLOCK.instant());
        ReflectionTestUtils.setField(library, "id", 7L);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.findById(7L)).thenReturn(Optional.of(library));
        LibraryService service = service();

        // when
        service.rename(1L, 7L, "올해읽을책");

        // then
        verify(libraryRepository, never()).rename(any());
        assertThat(library.getUpdatedAt()).isNull();
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 책장 이름 변경은 책장 없음 오류로 끝난다")
    void should_RejectRename_When_LibraryIsMissing() {
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().rename(1L, 7L, "읽을책"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND)
                );
        verify(libraryRepository, never()).rename(any());
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("다른 회원의 책장은 이름을 변경할 수 없다")
    void should_RejectRename_When_LibraryBelongsToAnotherMember() {
        Library library = Library.create(2L, "다른사람", CLOCK.instant());
        ReflectionTestUtils.setField(library, "id", 7L);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithNickname()));
        when(libraryRepository.findById(7L)).thenReturn(Optional.of(library));

        assertThatThrownBy(() -> service().rename(1L, 7L, "읽을책"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        assertThat(library.getName()).isEqualTo("다른사람");
        verify(libraryRepository, never()).rename(any());
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("비활성 회원의 책장 이름 변경은 책장을 조회하기 전에 거절한다")
    void should_RejectRename_When_MemberIsInactive() {
        Member inactiveMember = memberWithoutNickname();
        inactiveMember.withdraw(LocalDateTime.now());
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inactiveMember));

        assertThatThrownBy(() -> service().rename(1L, 7L, "읽을책"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(libraryRepository);
        assertThat(appender.list).isEmpty();
    }

    @Test
    @DisplayName("일괄 완독 대상 중 전체 페이지가 없는 책이 있으면 어느 항목도 변경하지 않는다")
    void should_NotChangeAnyItem_When_BulkFinishContainsUnknownTotalPages() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        LibraryItem knownItem = LibraryItem.create(1L, 2L, ReadingStatus.READING, 100, CLOCK.instant());
        LibraryItem unknownItem = LibraryItem.create(1L, 3L, ReadingStatus.READING, null, CLOCK.instant());
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, List.of(2L, 3L)))
                .thenReturn(List.of(knownItem, unknownItem));
        when(libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(1L, List.of(2L, 3L)))
                .thenReturn(List.of(knownItem, unknownItem));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book(100)));
        when(bookRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(book(null)));
        LibraryService service = service();

        // when & then
        assertThatThrownBy(() -> service.bulkChangeStatus(1L, List.of(2L, 3L), ReadingStatus.FINISHED))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_READING_STATE)
        );
        assertThat(knownItem.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(unknownItem.getStatus()).isEqualTo(ReadingStatus.READING);
    }

    @Test
    @DisplayName("일괄 완독은 책을 먼저 잠근 뒤 서재 항목을 잠근다")
    void should_LockBooksBeforeLibraryItems_When_BulkFinishing() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(activeMember()));
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 100, CLOCK.instant());
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, List.of(2L)))
                .thenReturn(List.of(item));
        when(bookRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(book(100)));
        when(libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(1L, List.of(2L)))
                .thenReturn(List.of(item));
        LibraryService service = service();

        // when
        service.bulkChangeStatus(1L, List.of(2L), ReadingStatus.FINISHED);

        // then
        InOrder lockOrder = inOrder(memberRepository, libraryItemRepository, bookRepository);
        lockOrder.verify(memberRepository).findByIdForUpdate(1L);
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
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(Optional.of(targetBook));
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        ))
                .thenReturn(Optional.empty());
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        ))
                .thenReturn(Optional.empty());
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
        LibraryItem sameRatedItem = LibraryItem.create(
                1L,
                9L,
                ReadingStatus.READING,
                null,
                CLOCK.instant()
        );
        sameRatedItem.rate(new BigDecimal("4.5"), CLOCK.instant().plusSeconds(10));
        Book sameRatedBook = mock(Book.class);
        when(sameRatedBook.getIsbn13()).thenReturn(new Isbn13("9788925568683"));
        when(sameRatedBook.getTitle()).thenReturn("같은 별점 도서");
        when(sameRatedBook.getCoverImageUrl()).thenReturn("https://example.com/cover.jpg");
        when(sameRatedBook.getAuthors()).thenReturn(List.of("작가"));
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(Optional.of(targetBook));
        when(libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        ))
                .thenReturn(Optional.of(sameRatedItem));
        when(bookRepository.findById(9L)).thenReturn(Optional.of(sameRatedBook));
        LibraryService service = service();

        // when
        var response = service.compareRatingsByIsbn13(1L, ISBN13, new BigDecimal("4.5"));

        // then
        assertThat(response.current()).extracting("bookId", "myRating", "ratingUpdatedAt")
                .containsExactly(9L, new BigDecimal("4.5"), CLOCK.instant().plusSeconds(10));
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        );
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        );
        verify(libraryItemRepository)
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                1L,
                10L,
                new BigDecimal("4.5")
        );
    }

    @Test
    @DisplayName("댓글순 서재 조회는 감상·답글 집계가 많은 책부터 반환한다")
    void should_OrderByCommentCount_When_ListingLibraryWithCommentSort() {
        // given
        LibraryItem firstItem = LibraryItem.create(
                1L,
                2L,
                ReadingStatus.WANT_TO_READ,
                null,
                CLOCK.instant()
        );
        LibraryItem secondItem = LibraryItem.create(
                1L,
                3L,
                ReadingStatus.WANT_TO_READ,
                null,
                CLOCK.instant()
        );
        Book firstBook = mock(Book.class);
        Book secondBook = mock(Book.class);
        when(firstBook.getId()).thenReturn(2L);
        when(secondBook.getId()).thenReturn(3L);
        when(firstBook.getIsbn13()).thenReturn(new Isbn13("9788925568683"));
        when(secondBook.getIsbn13()).thenReturn(new Isbn13("9788936433598"));
        defaultItems(List.of(firstItem, secondItem));
        when(bookRepository.findAllById(List.of(2L, 3L))).thenReturn(List.of(firstBook, secondBook));
        when(commentCountReader.getCommentCounts(anyCollection()))
                .thenReturn(Map.of(2L, 1L, 3L, 5L));
        LibraryService service = service();

        // when
        var response = service.getLibrary(1L, 1, null, LibrarySort.COMMENT);

        // then
        assertThat(response.items()).extracting("bookId", "commentCount")
                .containsExactly(tuple(3L, 5L), tuple(2L, 1L));
    }

    @Test
    @DisplayName("기본 책장에 배치된 독서 기록만 조회한다")
    void should_ListPlacedReadingRecords_When_ListingDefaultLibrary() {
        // given
        LibraryItem activeItem = LibraryItem.create(1L, 2L, ReadingStatus.READING, null, CLOCK.instant());
        defaultItems(List.of(activeItem));
        Book activeBook = bookWithId(2L);
        when(bookRepository.findAllById(List.of(2L))).thenReturn(List.of(activeBook));
        when(commentCountReader.getCommentCounts(anyCollection())).thenReturn(Map.of());

        // when
        var response = service().getLibrary(1L, 1, null, LibrarySort.RECENT);

        // then
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.items()).extracting("bookId").containsExactly(2L);
    }

    private void defaultItems(List<LibraryItem> items) {
        Library library = mock(Library.class);
        when(library.getId()).thenReturn(7L);
        when(libraryRepository.findByMemberIdAndIsDefaultTrue(1L)).thenReturn(Optional.of(library));
        List<LibraryBook> placements = items
                .stream()
                .map(item -> LibraryBook.create(7L, item.getBookId(), item.getBookId()))
                .toList();
        when(libraryBookRepository.findAllByLibraryId(7L)).thenReturn(placements);
        List<Long> bookIds = items
                .stream()
                .map(LibraryItem::getBookId)
                .toList();
        when(libraryItemRepository.findAllByMemberIdAndBookIdIn(1L, bookIds)).thenReturn(items);
    }

    @Test
    @DisplayName("활성 회원의 빈 공개 서재는 빈 목록을 반환한다")
    void should_ReturnEmptyPublicLibrary_When_ActiveMemberHasNoItems() {
        // given
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getDisplayName()).thenReturn("책책이");
        when(member.getProfileImageUrl()).thenReturn("https://example.com/profile.jpg");
        when(member.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(commentCountReader.getCommentCounts(Set.of())).thenReturn(Map.of());

        // when
        var response = service().getPublicLibrary(1L, 1, null, LibrarySort.RECENT);

        // then
        assertThat(response.totalCount()).isZero();
        assertThat(response.filteredCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.member()).extracting("memberId", "displayName", "profileImageUrl")
                .containsExactly(1L, "책책이", "https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("존재하지 않거나 비활성 상태인 회원의 공개 서재는 조회할 수 없다")
    void should_RejectPublicLibrary_When_MemberIsMissingOrInactive() {
        // given
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertLibraryNotFound(1L);

        for (AccountStatus status : List.of(AccountStatus.WITHDRAWN, AccountStatus.SUSPENDED)) {
            Member member = mock(Member.class);
            when(member.getAccountStatus()).thenReturn(status);
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
            assertLibraryNotFound(1L);
        }
    }

    @Test
    @DisplayName("ISBN으로 내 별점만 삭제하고 독서 기록을 보존한다")
    void should_RemoveOnlyMyRating_When_DeletingRatingByIsbn() {
        // given
        Book book = book(240);
        ReflectionTestUtils.setField(book, "id", 10L);
        LibraryItem item = LibraryItem.create(1L, 10L, ReadingStatus.READING, 240, CLOCK.instant());
        item.changeCurrentPage(23, 240, CLOCK.instant());
        item.rate(new BigDecimal("4.5"), CLOCK.instant());
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithoutNickname()));
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(Optional.of(book));
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(1L, 10L)).thenReturn(Optional.of(item));

        // when
        service().removeRatingByIsbn13(1L, ISBN13);

        // then
        assertThat(item.getRating()).isNull();
        assertThat(item.getRatingUpdatedAt()).isNull();
        assertThat(item.getStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(item.getCurrentPage()).isEqualTo(23);
        assertThat(item.getReadingUpdatedAt()).isEqualTo(CLOCK.instant());
        InOrder order = inOrder(memberRepository, bookRepository, libraryItemRepository);
        order.verify(memberRepository).findByIdForUpdate(1L);
        order.verify(bookRepository).findByIsbn13(ISBN13);
        order.verify(libraryItemRepository).findByMemberIdAndBookIdForUpdate(1L, 10L);
        verifyNoInteractions(bookResolver, libraryBookRepository);
    }

    @Test
    @DisplayName("미등록 ISBN은 외부 조회나 생성 없이 도서 없음 오류를 반환한다")
    void should_RejectWithoutResolvingBook_When_DeletingUnregisteredIsbnRating() {
        // given
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithoutNickname()));

        // when & then
        assertThatThrownBy(() -> service().removeRatingByIsbn13(1L, ISBN13))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.BOOK_NOT_FOUND);
        verifyNoInteractions(bookResolver, libraryItemRepository);
    }

    @Test
    @DisplayName("공유 기록이 없는 도서의 ISBN 별점 삭제는 기록을 만들지 않는다")
    void should_LeaveRecordsEmpty_When_DeletingRatingWithoutRecord() {
        // given
        Book book = book(240);
        ReflectionTestUtils.setField(book, "id", 10L);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(memberWithoutNickname()));
        when(bookRepository.findByIsbn13(ISBN13)).thenReturn(Optional.of(book));

        // when
        service().removeRatingByIsbn13(1L, ISBN13);

        // then
        verify(libraryItemRepository).findByMemberIdAndBookIdForUpdate(1L, 10L);
        verify(libraryItemRepository, never()).saveAndFlush(any());
        verifyNoInteractions(bookResolver, libraryBookRepository);
    }

    @Test
    @DisplayName("비활성 회원은 ISBN 조회 전에 별점 삭제를 거절한다")
    void should_RejectBeforeLookup_When_InactiveMemberDeletesIsbnRating() {
        // given
        Member member = memberWithoutNickname();
        ReflectionTestUtils.setField(member, "accountStatus", AccountStatus.SUSPENDED);
        when(memberRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> service().removeRatingByIsbn13(1L, ISBN13))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(bookRepository, bookResolver, libraryItemRepository);
    }

    private void assertLibraryNotFound(long memberId) {
        assertThatThrownBy(() -> service().getPublicLibrary(memberId, 1, null, LibrarySort.RECENT))
                .isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_NOT_FOUND)
        );
    }

    private Member activeMember() {
        return Member.create("익명", null, LocalDateTime.of(2026, 10, 23, 12, 0));
    }

    private LibraryService service() {
        return new LibraryService(
                libraryRepository,
                libraryItemRepository,
                libraryBookRepository,
                bookRepository,
                bookResolver,
                commentCountReader,
                memberRepository,
                CLOCK,
                transactionManager
        );
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

    private Book bookWithId(long bookId) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(bookId);
        when(book.getIsbn13()).thenReturn(ISBN13);
        when(book.getTitle()).thenReturn("마션");
        when(book.getAuthors()).thenReturn(List.of());
        when(book.getTranslators()).thenReturn(List.of());
        return book;
    }

    private Member memberWithNickname() {
        Member member = memberWithoutNickname();
        member.updateNickname("책읽는사람");
        return member;
    }

    private Member memberWithoutNickname() {
        return Member.create("익명 회원", null, LocalDateTime.of(2026, 8, 14, 0, 0));
    }
}
