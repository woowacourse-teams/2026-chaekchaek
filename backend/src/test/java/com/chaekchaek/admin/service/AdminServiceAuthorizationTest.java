package com.chaekchaek.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.admin.authorization.AdminAuthorizationAspect;
import com.chaekchaek.admin.domain.RecommendedBook;
import com.chaekchaek.admin.dto.RecommendedBookListResponse;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.repository.RecommendedBookRepository;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;

class AdminServiceAuthorizationTest {

    private static final long BOOK_ID = 3L;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-28T00:00:00Z"), ZoneOffset.UTC);
    private static final Isbn13 ISBN13 = new Isbn13("9788925568683");
    private static final CurrentActor ADMIN_ACTOR = CurrentActor.admin(1L, 1L);
    private static final CurrentActor MEMBER_ACTOR = CurrentActor.member(2L, 2L);

    private final RecommendedBookRepository recommendedBookRepository = mock(RecommendedBookRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final BookResolver bookResolver = mock(BookResolver.class);
    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        AdminService target = new AdminService(
                recommendedBookRepository,
                bookRepository,
                bookResolver,
                currentActorProvider,
                CLOCK,
                transactionManager
        );
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AdminAuthorizationAspect(currentActorProvider));
        adminService = factory.getProxy();
    }

    @ParameterizedTest
    @MethodSource("nonAdminActors")
    @DisplayName("관리자가 아닌 사용자의 추천 도서 등록은 도서 조회 전에 거절한다")
    void should_RejectAddingRecommendedBook_When_ActorIsNotAdmin(CurrentActor actor) {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(actor);

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(bookResolver, bookRepository, recommendedBookRepository, transactionManager);
    }

    @ParameterizedTest
    @MethodSource("nonAdminActors")
    @DisplayName("관리자가 아닌 사용자의 도서 ID 직접 등록은 도서 조회 전에 거절한다")
    void should_RejectAddingRecommendedBookById_When_ActorIsNotAdmin(CurrentActor actor) {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(actor);

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBook(BOOK_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(bookResolver, bookRepository, recommendedBookRepository, transactionManager);
    }

    @ParameterizedTest
    @MethodSource("nonAdminActors")
    @DisplayName("관리자가 아닌 사용자의 추천 도서 삭제는 추천 도서 조회 전에 거절한다")
    void should_RejectDeletingRecommendedBook_When_ActorIsNotAdmin(CurrentActor actor) {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(actor);

        // when & then
        assertThatThrownBy(() -> adminService.deleteRecommendedBook(BOOK_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
        verifyNoInteractions(bookResolver, bookRepository, recommendedBookRepository, transactionManager);
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 추천 도서 등록은 도서 조회 전에 거절한다")
    void should_RejectAddingRecommendedBook_When_ActorIsNotAuthenticated() {
        // given
        when(currentActorProvider.getCurrentActor()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> adminService.addRecommendedBookByIsbn13(ISBN13))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
                );
        verifyNoInteractions(bookResolver, bookRepository, recommendedBookRepository, transactionManager);
    }

    @Test
    @DisplayName("인증되지 않은 사용자의 추천 도서 삭제는 추천 도서 조회 전에 거절한다")
    void should_RejectDeletingRecommendedBook_When_ActorIsNotAuthenticated() {
        // given
        when(currentActorProvider.getCurrentActor()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> adminService.deleteRecommendedBook(BOOK_ID))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
                );
        verifyNoInteractions(bookResolver, bookRepository, recommendedBookRepository, transactionManager);
    }

    @Test
    @DisplayName("관리자는 프록시의 권한 검사를 거쳐 추천 도서를 등록한다")
    void should_AddRecommendedBook_When_ActorIsAdmin() {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(ADMIN_ACTOR);
        Book book = book();
        RecommendedBook recommendedBook = RecommendedBook.create(BOOK_ID, CLOCK.instant());
        when(bookResolver.findOrCreate(ISBN13)).thenReturn(book);
        when(bookRepository.findDetailById(BOOK_ID)).thenReturn(Optional.of(book));
        when(recommendedBookRepository.saveAndFlush(any(RecommendedBook.class))).thenReturn(recommendedBook);

        // when
        RecommendedBookResponse response = adminService.addRecommendedBookByIsbn13(ISBN13);

        // then
        assertThat(response.bookId()).isEqualTo(BOOK_ID);
        verify(currentActorProvider).getCurrentActor();
        verify(bookResolver).findOrCreate(ISBN13);
        verify(recommendedBookRepository).saveAndFlush(any(RecommendedBook.class));
    }

    private Book book() {
        Book book = mock(Book.class);
        when(book.getId()).thenReturn(BOOK_ID);
        when(book.getIsbn13()).thenReturn(ISBN13);
        when(book.getTitle()).thenReturn("마션");
        when(book.getCoverImageUrl()).thenReturn("https://example.com/martian.jpg");
        when(book.getAuthors()).thenReturn(List.of("앤디 위어"));
        return book;
    }

    @Test
    @DisplayName("관리자는 프록시의 권한 검사를 거쳐 추천 도서를 삭제한다")
    void should_DeleteRecommendedBook_When_ActorIsAdmin() {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(ADMIN_ACTOR);
        RecommendedBook recommendedBook = RecommendedBook.create(BOOK_ID, CLOCK.instant());
        when(recommendedBookRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(recommendedBook));

        // when
        adminService.deleteRecommendedBook(BOOK_ID);

        // then
        verify(currentActorProvider).getCurrentActor();
        verify(recommendedBookRepository).delete(recommendedBook);
    }

    @Test
    @DisplayName("공개 추천 도서 조회는 프록시를 거쳐도 인증 정보를 요구하지 않는다")
    void should_ReturnRecommendedBooks_When_ActorIsNotAuthenticated() {
        // given
        when(currentActorProvider.getCurrentActor()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
        when(recommendedBookRepository.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());

        // when
        RecommendedBookListResponse response = adminService.getRecommendedBooks();

        // then
        assertThat(response.books()).isEmpty();
        verifyNoInteractions(currentActorProvider, bookResolver, bookRepository, transactionManager);
    }

    private static Stream<CurrentActor> nonAdminActors() {
        return Stream.of(MEMBER_ACTOR, CurrentActor.guest(3L));
    }
}
