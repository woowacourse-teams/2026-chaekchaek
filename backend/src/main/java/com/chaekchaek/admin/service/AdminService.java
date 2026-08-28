package com.chaekchaek.admin.service;

import com.chaekchaek.admin.domain.RecommendedBook;
import com.chaekchaek.admin.dto.RecommendedBookListResponse;
import com.chaekchaek.admin.dto.RecommendedBookResponse;
import com.chaekchaek.admin.repository.RecommendedBookRepository;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminService {

    private static final int MAX_RECOMMENDED_BOOKS = 10;

    private final RecommendedBookRepository recommendedBookRepository;
    private final BookRepository bookRepository;
    private final BookResolver bookResolver;
    private final CurrentActorProvider currentActorProvider;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public AdminService(
            RecommendedBookRepository recommendedBookRepository,
            BookRepository bookRepository,
            BookResolver bookResolver,
            CurrentActorProvider currentActorProvider,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.recommendedBookRepository = recommendedBookRepository;
        this.bookRepository = bookRepository;
        this.bookResolver = bookResolver;
        this.currentActorProvider = currentActorProvider;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public RecommendedBookListResponse getRecommendedBooks() {
        requireAdmin();
        List<RecommendedBook> recommendedBooks = recommendedBookRepository.findAllByOrderByCreatedAtDescIdDesc();
        Map<Long, Book> books = booksWithAuthorsById(recommendedBooks.stream()
                .map(RecommendedBook::getBookId).toList());
        List<RecommendedBookResponse> responses = recommendedBooks.stream()
                .map(recommendedBook -> toResponse(recommendedBook, books.get(recommendedBook.getBookId())))
                .filter(Objects::nonNull)
                .toList();
        return new RecommendedBookListResponse(responses);
    }

    public RecommendedBookResponse addRecommendedBookByIsbn13(String isbn13) {
        requireAdmin();
        Book book = bookResolver.findOrCreate(isbn13);
        return transactionTemplate.execute(status -> addRecommendedBook(book.getId()));
    }

    @Transactional
    public RecommendedBookResponse addRecommendedBook(long bookId) {
        Book book = bookRepository.findDetailById(bookId).orElseThrow(BookNotFoundException::new);
        if (recommendedBookRepository.existsByBookId(bookId)) {
            throw new BusinessException(ErrorCode.RECOMMENDED_BOOK_ALREADY_EXISTS);
        }
        if (recommendedBookRepository.count() >= MAX_RECOMMENDED_BOOKS) {
            throw new BusinessException(ErrorCode.RECOMMENDED_BOOK_LIMIT_EXCEEDED);
        }
        return toResponse(save(bookId), book);
    }

    @Transactional
    public void deleteRecommendedBook(long bookId) {
        requireAdmin();
        RecommendedBook recommendedBook = recommendedBookRepository.findByBookId(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECOMMENDED_BOOK_NOT_FOUND));
        recommendedBookRepository.delete(recommendedBook);
    }

    private void requireAdmin() {
        if (!currentActorProvider.getCurrentActor().isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private RecommendedBook save(long bookId) {
        try {
            return recommendedBookRepository.saveAndFlush(RecommendedBook.create(bookId, clock.instant()));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.RECOMMENDED_BOOK_ALREADY_EXISTS);
        }
    }

    private Map<Long, Book> booksWithAuthorsById(List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllWithAuthorsByIdIn(bookIds).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, book -> book));
    }

    private RecommendedBookResponse toResponse(RecommendedBook recommendedBook, Book book) {
        if (book == null) {
            return null;
        }
        return new RecommendedBookResponse(book.getId(), book.getIsbn13(), book.getTitle(), book.getCoverImageUrl(),
                book.getAuthors(), recommendedBook.getCreatedAt());
    }
}
