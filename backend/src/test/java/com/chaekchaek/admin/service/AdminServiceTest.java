package com.chaekchaek.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chaekchaek.admin.domain.RecommendedBook;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.repository.RecommendedBookRepository;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class AdminServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC);
    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");
    private static final CurrentActor ADMIN_ACTOR = CurrentActor.admin(1L, 1L);
    private static final CurrentActor MEMBER_ACTOR = CurrentActor.member(2L, 2L);

    @Test
    @DisplayName("최근에 추천한 순서로 책 정보를 반환한다")
    void should_ReturnBooksInLatestRecommendedOrder_When_FindingRecommendedBooks() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, mock(BookResolver.class));
        List<RecommendedBook> recommendedBooks = List.of(
                recommendedBook(3L, "2026-08-28T00:00:00Z"), recommendedBook(1L, "2026-08-27T00:00:00Z")
        );
        List<Book> books = List.of(book(1L, "첫 번째 책"), book(3L, "세 번째 책"));
        when(recommendedBookRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(recommendedBooks);
        when(bookRepository.findAllWithAuthorsByIdIn(List.of(3L, 1L))).thenReturn(books);

        // when
        List<RecommendedBookResponse> result = adminService.getRecommendedBooks().books();

        // then
        assertThat(result).extracting(RecommendedBookResponse::bookId).containsExactly(3L, 1L);
        assertThat(result).extracting(RecommendedBookResponse::title).containsExactly("세 번째 책", "첫 번째 책");
        assertThat(result).extracting(RecommendedBookResponse::isbn13)
                .containsExactly("9780000000026", "9780000000002");
        assertThat(result).extracting(RecommendedBookResponse::createdAt)
                .containsExactly(Instant.parse("2026-08-28T00:00:00Z"), Instant.parse("2026-08-27T00:00:00Z"));
    }

    @Test
    @DisplayName("추천한 책이 없으면 빈 목록을 반환한다")
    void should_ReturnEmptyBooks_When_NoBookIsRecommended() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, mock(BookResolver.class));
        when(recommendedBookRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());

        // when
        List<RecommendedBookResponse> result = adminService.getRecommendedBooks().books();

        // then
        assertThat(result).isEmpty();
        verify(bookRepository, never()).findAllWithAuthorsByIdIn(any());
    }

    @Test
    @DisplayName("ISBN13으로 조회한 책을 추천 도서로 등록한다")
    void should_SaveRecommendedBook_When_AddingByIsbn13() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, bookResolver);
        Book book = book(3L, "세 번째 책");
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findDetailById(3L)).thenReturn(Optional.of(book));
        when(recommendedBookRepository.existsByBookId(3L)).thenReturn(false);
        when(recommendedBookRepository.count()).thenReturn(9L);
        when(recommendedBookRepository.saveAndFlush(any(RecommendedBook.class)))
                .thenReturn(RecommendedBook.create(3L, CLOCK.instant()));

        // when
        RecommendedBookResponse result = adminService.addRecommendedBookByIsbn13(ISBN13);

        // then
        assertThat(result.bookId()).isEqualTo(3L);
        assertThat(result.title()).isEqualTo("세 번째 책");
        assertThat(result.createdAt()).isEqualTo(CLOCK.instant());
        verify(recommendedBookRepository).saveAndFlush(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("이미 추천 중인 책이라면 추천 도서로 등록하지 않는다")
    void should_NotSaveRecommendedBook_When_BookIsAlreadyRecommended() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, bookResolver);
        Book book = book(3L, "세 번째 책");
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findDetailById(3L)).thenReturn(Optional.of(book));
        when(recommendedBookRepository.existsByBookId(3L)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RECOMMENDED_BOOK_ALREADY_EXISTS));
        verify(recommendedBookRepository, never()).saveAndFlush(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("이미 10권을 추천했다면 추천 도서로 등록하지 않는다")
    void should_NotSaveRecommendedBook_When_RecommendedBookLimitIsReached() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, bookResolver);
        Book book = book(3L, "세 번째 책");
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findDetailById(3L)).thenReturn(Optional.of(book));
        when(recommendedBookRepository.existsByBookId(3L)).thenReturn(false);
        when(recommendedBookRepository.count()).thenReturn(10L);

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RECOMMENDED_BOOK_LIMIT_EXCEEDED));
        verify(recommendedBookRepository, never()).saveAndFlush(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("조회한 책이 저장되어 있지 않다면 추천 도서로 등록하지 않는다")
    void should_NotSaveRecommendedBook_When_BookIsNotStored() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookRepository bookRepository = mock(BookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        AdminService adminService = adminService(recommendedBookRepository, bookRepository, bookResolver);
        Book book = book(3L, "세 번째 책");
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findDetailById(3L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOf(BookNotFoundException.class);
        verify(recommendedBookRepository, never()).saveAndFlush(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("추천 중인 책이라면 추천 도서에서 삭제한다")
    void should_DeleteRecommendedBook_When_BookIsRecommended() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, mock(BookRepository.class),
                mock(BookResolver.class));
        RecommendedBook recommendedBook = recommendedBook(3L, "2026-08-28T00:00:00Z");
        when(recommendedBookRepository.findByBookId(3L)).thenReturn(Optional.of(recommendedBook));

        // when
        adminService.deleteRecommendedBook(3L);

        // then
        verify(recommendedBookRepository).delete(recommendedBook);
    }

    @Test
    @DisplayName("추천 중인 책이 아니라면 삭제하지 않는다")
    void should_NotDeleteRecommendedBook_When_BookIsNotRecommended() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, mock(BookRepository.class),
                mock(BookResolver.class));
        when(recommendedBookRepository.findByBookId(3L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.deleteRecommendedBook(3L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.RECOMMENDED_BOOK_NOT_FOUND));
        verify(recommendedBookRepository, never()).delete(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("관리자가 아니라면 추천 도서를 조회하지 않는다")
    void should_NotReturnRecommendedBooks_When_ActorIsNotAdmin() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, mock(BookRepository.class),
                mock(BookResolver.class), actorProvider(MEMBER_ACTOR));

        // when & then
        assertThatThrownBy(adminService::getRecommendedBooks)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(recommendedBookRepository, never()).findAllByOrderByCreatedAtDescIdDesc();
    }

    @Test
    @DisplayName("관리자가 아니라면 도서를 조회하지 않고 추천 도서 등록을 거절한다")
    void should_NotSaveRecommendedBook_When_ActorIsNotAdmin() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        BookResolver bookResolver = mock(BookResolver.class);
        AdminService adminService = adminService(recommendedBookRepository, mock(BookRepository.class),
                bookResolver, actorProvider(MEMBER_ACTOR));

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(bookResolver, never()).findOrCreate(ISBN13);
        verify(recommendedBookRepository, never()).saveAndFlush(any(RecommendedBook.class));
    }

    @Test
    @DisplayName("관리자가 아니라면 추천 도서를 삭제하지 않는다")
    void should_NotDeleteRecommendedBook_When_ActorIsNotAdmin() {
        // given
        RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
        AdminService adminService = adminService(recommendedBookRepository, mock(BookRepository.class),
                mock(BookResolver.class), actorProvider(MEMBER_ACTOR));

        // when & then
        assertThatThrownBy(() -> adminService.deleteRecommendedBook(3L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verify(recommendedBookRepository, never()).delete(any(RecommendedBook.class));
    }

    private static AdminService adminService(RecommendedBookRepository recommendedBookRepository,
                                             BookRepository bookRepository, BookResolver bookResolver) {
        return adminService(recommendedBookRepository, bookRepository, bookResolver, actorProvider(ADMIN_ACTOR));
    }

    private static AdminService adminService(RecommendedBookRepository recommendedBookRepository,
                                             BookRepository bookRepository, BookResolver bookResolver,
                                             CurrentActorProvider currentActorProvider) {
        return new AdminService(recommendedBookRepository, bookRepository, bookResolver, currentActorProvider, CLOCK,
                mock(PlatformTransactionManager.class));
    }

    private static CurrentActorProvider actorProvider(CurrentActor actor) {
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
        when(currentActorProvider.getCurrentActor()).thenReturn(actor);
        return currentActorProvider;
    }

    private static RecommendedBook recommendedBook(long bookId, String createdAt) {
        return RecommendedBook.create(bookId, Instant.parse(createdAt));
    }

    private static Book book(long id, String title) {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(id);
        when(book.getIsbn13()).thenReturn(isbn13(id));
        when(book.getTitle()).thenReturn(title);
        when(book.getCoverImageUrl()).thenReturn("https://example.com/" + id + ".jpg");
        when(book.getAuthors()).thenReturn(List.of("저자 " + id));
        return book;
    }

    private static Isbn13 isbn13(long id) {
        return switch ((int) id) {
            case 1 -> new Isbn13("9780000000002");
            case 2 -> new Isbn13("9780000000019");
            case 3 -> new Isbn13("9780000000026");
            default -> new Isbn13("9788925568683");
        };
    }
}
