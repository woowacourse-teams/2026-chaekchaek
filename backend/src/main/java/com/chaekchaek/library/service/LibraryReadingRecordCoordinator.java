package com.chaekchaek.library.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class LibraryReadingRecordCoordinator implements ReadingRecordCoordinator {

    private final BookRepository bookRepository;
    private final LibraryItemRepository libraryItemRepository;
    private final Clock clock;

    @Override
    public void recordReview(long memberId, long bookId, Integer currentPage, Integer totalPages) {
        Book book = lockedBook(bookId);
        rememberTotalPages(book, totalPages);
        if (currentPage != null && book.getTotalPages() == null) {
            throw invalidReadingState();
        }
        LibraryItem item = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .orElseGet(() -> libraryItemRepository.save(
                        LibraryItem.create(memberId, bookId, ReadingStatus.WANT_TO_READ, null, clock.instant())));
        if (currentPage != null && currentPage > item.getCurrentPage()) {
            item.changeCurrentPage(currentPage, book.getTotalPages(), clock.instant());
        }
    }

    @Override
    public void validateReviewPage(long bookId, Integer currentPage, Integer totalPages) {
        if (currentPage == null) {
            return;
        }
        Book book = lockedBook(bookId);
        rememberTotalPages(book, totalPages);
        if (book.getTotalPages() == null || currentPage < 0 || currentPage > book.getTotalPages()) {
            throw invalidReadingState();
        }
    }

    private Book lockedBook(long bookId) {
        return bookRepository.findByIdForUpdate(bookId).orElseThrow(BookNotFoundException::new);
    }

    private void rememberTotalPages(Book book, Integer totalPages) {
        if (totalPages != null) {
            book.rememberTotalPages(totalPages);
        }
    }

    private BusinessException invalidReadingState() {
        return new BusinessException(ErrorCode.INVALID_READING_STATE);
    }
}
