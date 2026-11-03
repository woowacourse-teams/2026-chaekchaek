package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class LibraryBookServiceTest {

    private static final long MEMBER_ID = 1L;
    private static final long LIBRARY_ID = 7L;
    private static final long BOOK_ID = 10L;
    private static final Isbn13 ISBN13 = new Isbn13("9788936433598");
    private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("도서를 소유한 책장의 맨 뒤에 배치하고 독서 기록을 함께 생성한다")
    void should_CreatePlacementAndReadingRecord_When_BookIsAdded() {
        // given
        Library library = library(MEMBER_ID);
        Book book = book(368);
        MemberRepository memberRepository = mock(MemberRepository.class);
        LibraryRepository libraryRepository = mock(LibraryRepository.class);
        LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        BookCommentCountReader commentCountReader = mock(BookCommentCountReader.class);
        PlatformTransactionManager transactionManager = transactionManager();
        when(libraryRepository.findById(LIBRARY_ID)).thenReturn(Optional.of(library));
        when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(activeMember()));
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
        when(libraryBookRepository.existsByLibraryIdAndBookId(LIBRARY_ID, BOOK_ID)).thenReturn(false);
        when(libraryBookRepository.countByLibraryId(LIBRARY_ID)).thenReturn(4L);
        when(libraryBookRepository.findMaxDisplayOrderByLibraryId(LIBRARY_ID)).thenReturn(8L);
        when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(MEMBER_ID, BOOK_ID))
                .thenReturn(Optional.empty());
        when(libraryItemRepository.saveAndFlush(any(LibraryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(commentCountReader.getCommentCounts(anyCollection())).thenReturn(Map.of(BOOK_ID, 3L));
        LibraryBookService service = service(
                libraryRepository,
                libraryBookRepository,
                libraryItemRepository,
                bookRepository,
                bookResolver,
                commentCountReader,
                memberRepository,
                transactionManager
        );

        // when
        var response = service.addByIsbn13(MEMBER_ID, LIBRARY_ID, ISBN13, ReadingStatus.READING, 368);

        // then
        assertThat(response.bookId()).isEqualTo(BOOK_ID);
        assertThat(response.status()).isEqualTo(ReadingStatus.READING);
        assertThat(response.currentPage()).isZero();
        assertThat(response.commentCount()).isEqualTo(3L);
        ArgumentCaptor<LibraryItem> itemCaptor = ArgumentCaptor.forClass(LibraryItem.class);
        verify(libraryItemRepository).saveAndFlush(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo(ReadingStatus.READING);
        ArgumentCaptor<LibraryBook> placementCaptor = ArgumentCaptor.forClass(LibraryBook.class);
        verify(libraryBookRepository).create(placementCaptor.capture());
        assertThat(placementCaptor.getValue().getDisplayOrder()).isEqualTo(9L);
        InOrder lockOrder = inOrder(memberRepository, bookRepository, libraryItemRepository, libraryBookRepository);
        lockOrder.verify(memberRepository).findByIdForUpdate(MEMBER_ID);
        lockOrder.verify(bookRepository).findByIdForUpdate(BOOK_ID);
        lockOrder.verify(libraryItemRepository).findByMemberIdAndBookIdForUpdate(MEMBER_ID, BOOK_ID);
        lockOrder.verify(libraryBookRepository).create(any(LibraryBook.class));
    }

    @Test
    @DisplayName("이미 독서 기록이 있으면 요청 상태와 페이지를 적용하지 않는다")
    void should_PreserveExistingReadingState_When_AddingPlacement() {
        // given
        LibraryItem item = LibraryItem.create(MEMBER_ID, BOOK_ID, ReadingStatus.READING, 368, NOW.minusSeconds(60));
        item.changeCurrentPage(120, 368, NOW.minusSeconds(60));
        item.rate(new BigDecimal("4.5"), NOW.minusSeconds(60));
        TestContext context = context(library(MEMBER_ID), book(368), item, 0L);

        // when
        var response = context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.WANT_TO_READ,
                null
        );

        // then
        assertThat(response.status()).isEqualTo(ReadingStatus.READING);
        assertThat(response.currentPage()).isEqualTo(120);
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(item.getReadingUpdatedAt()).isEqualTo(NOW.minusSeconds(60));
        verify(context.libraryItemRepository(), never()).saveAndFlush(any(LibraryItem.class));
    }

    @Test
    @DisplayName("독서 상태가 초기화된 기존 기록을 다시 시작하고 별점은 유지한다")
    void should_RestartClearedReadingStateAndPreserveRating_When_AddingPlacement() {
        // given
        LibraryItem item = LibraryItem.create(MEMBER_ID, BOOK_ID, ReadingStatus.READING, 368, NOW.minusSeconds(60));
        item.rate(new BigDecimal("4.5"), NOW.minusSeconds(60));
        item.clearReadingState();
        TestContext context = context(library(MEMBER_ID), book(368), item, 0L);

        // when
        var response = context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.FINISHED,
                null
        );

        // then
        assertThat(response.status()).isEqualTo(ReadingStatus.FINISHED);
        assertThat(response.currentPage()).isEqualTo(368);
        assertThat(response.rating()).isEqualByComparingTo("4.5");
        assertThat(item.getReadingUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("책장에 이미 같은 도서가 있으면 배치 중복 오류를 반환한다")
    void should_RejectDuplicatePlacement_When_BookIsAlreadyInLibrary() {
        // given
        TestContext context = context(library(MEMBER_ID), book(368), null, 4L);
        when(context.libraryBookRepository().existsByLibraryIdAndBookId(LIBRARY_ID, BOOK_ID)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.READING,
                368
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS)
        );
        verify(context.libraryBookRepository(), never()).create(any(LibraryBook.class));
        verifyNoInteractions(context.libraryItemRepository());
    }

    @Test
    @DisplayName("책장에 30권이 있으면 새 배치를 거절한다")
    void should_RejectPlacement_When_LibraryIsAtCapacity() {
        // given
        TestContext context = context(library(MEMBER_ID), book(368), null, 30L);

        // when & then
        assertThatThrownBy(() -> context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.READING,
                368
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED)
        );
        verifyNoInteractions(context.libraryItemRepository());
        verify(context.libraryBookRepository(), never()).create(any(LibraryBook.class));
    }

    @Test
    @DisplayName("책장에 29권이 있으면 30번째 도서를 배치할 수 있다")
    void should_AllowPlacement_When_LibraryHas29Books() {
        // given
        TestContext context = context(library(MEMBER_ID), book(368), null, 29L);

        // when
        var response = context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.READING,
                368
        );

        // then
        assertThat(response.bookId()).isEqualTo(BOOK_ID);
        verify(context.libraryBookRepository()).create(any(LibraryBook.class));
    }

    @Test
    @DisplayName("다른 회원의 책장이라면 도서를 조회하거나 생성하지 않는다")
    void should_RejectForeignLibraryBeforeResolvingBook() {
        // given
        TestContext context = context(library(2L), book(368), null, 0L);

        // when & then
        assertThatThrownBy(() -> context.service().addByIsbn13(
                MEMBER_ID,
                LIBRARY_ID,
                ISBN13,
                ReadingStatus.READING,
                368
        )).isInstanceOfSatisfying(
                BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
        );
        verifyNoInteractions(context.bookResolver());
        verify(context.memberRepository(), never()).findByIdForUpdate(MEMBER_ID);
    }

    private TestContext context(Library library, Book book, LibraryItem item, long count) {
        LibraryRepository libraryRepository = mock(LibraryRepository.class);
        LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
        LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        BookCommentCountReader commentCountReader = mock(BookCommentCountReader.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        lenient().when(libraryRepository.findById(LIBRARY_ID)).thenReturn(Optional.of(library));
        lenient().when(memberRepository.findByIdForUpdate(MEMBER_ID)).thenReturn(Optional.of(activeMember()));
        lenient().when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        lenient().when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
        lenient().when(libraryBookRepository.existsByLibraryIdAndBookId(LIBRARY_ID, BOOK_ID)).thenReturn(false);
        lenient().when(libraryBookRepository.countByLibraryId(LIBRARY_ID)).thenReturn(count);
        lenient().when(libraryBookRepository.findMaxDisplayOrderByLibraryId(LIBRARY_ID)).thenReturn(0L);
        lenient().when(libraryItemRepository.findByMemberIdAndBookIdForUpdate(MEMBER_ID, BOOK_ID))
                .thenReturn(Optional.ofNullable(item));
        lenient().when(libraryItemRepository.saveAndFlush(any(LibraryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(commentCountReader.getCommentCounts(anyCollection())).thenReturn(Map.of());
        return new TestContext(
                service(libraryRepository, libraryBookRepository, libraryItemRepository, bookRepository,
                        bookResolver, commentCountReader, memberRepository, transactionManager()),
                libraryBookRepository,
                libraryItemRepository,
                bookResolver,
                memberRepository
        );
    }

    private LibraryBookService service(
            LibraryRepository libraryRepository,
            LibraryBookRepository libraryBookRepository,
            LibraryItemRepository libraryItemRepository,
            BookRepository bookRepository,
            BookResolver bookResolver,
            BookCommentCountReader commentCountReader,
            MemberRepository memberRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new LibraryBookService(
                libraryRepository,
                libraryBookRepository,
                libraryItemRepository,
                bookRepository,
                bookResolver,
                commentCountReader,
                memberRepository,
                CLOCK,
                transactionManager
        );
    }

    private PlatformTransactionManager transactionManager() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(invocation -> new SimpleTransactionStatus());
        return transactionManager;
    }

    private Library library(long memberId) {
        Library library = Library.create(memberId, "가을책장", NOW);
        ReflectionTestUtils.setField(library, "id", LIBRARY_ID);
        return library;
    }

    private Book book(Integer totalPages) {
        Book book = Book.create(
                ISBN13,
                "책 제목",
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
        );
        ReflectionTestUtils.setField(book, "id", BOOK_ID);
        return book;
    }

    private Member activeMember() {
        return Member.create("활성 테스트 회원", null, LocalDateTime.of(2026, 10, 23, 0, 0));
    }

    private record TestContext(
            LibraryBookService service,
            LibraryBookRepository libraryBookRepository,
            LibraryItemRepository libraryItemRepository,
            BookResolver bookResolver,
            MemberRepository memberRepository
    ) {
    }
}
