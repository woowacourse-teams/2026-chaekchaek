package com.chaekchaek.library.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.RatingComparisonBookResponse;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.repository.LibraryItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final int PAGE_SIZE = 10;

    private final LibraryItemRepository libraryItemRepository;
    private final BookRepository bookRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public LibraryListResponse getLibrary(long memberId, int page, ReadingStatus status, LibrarySort sort) {
        Pageable pageable = PageRequest.of(page - 1, PAGE_SIZE, sortOf(sort));
        Page<LibraryItem> result = status == null
                ? libraryItemRepository.findAllByMemberId(memberId, pageable)
                : libraryItemRepository.findAllByMemberIdAndStatus(memberId, status, pageable);
        long totalCount = libraryItemRepository.countByMemberId(memberId);
        long filteredCount = status == null ? totalCount
                : libraryItemRepository.countByMemberIdAndStatus(memberId, status);
        Integer nextPage = result.hasNext() ? page + 1 : null;
        Map<Long, Book> books = result.getContent().stream()
                .map(LibraryItem::getBookId)
                .collect(java.util.stream.Collectors.toMap(
                        bookId -> bookId,
                        this::getBook
                ));
        return new LibraryListResponse(totalCount, filteredCount, nextPage,
                result.getContent().stream()
                        .map(item -> LibraryItemResponse.from(item, books.get(item.getBookId())))
                        .toList());
    }

    @Transactional
    public LibraryItemResponse add(long memberId, long bookId, ReadingStatus status, Integer totalPages) {
        libraryItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .ifPresent(item -> { throw new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS); });
        Book book = getBook(bookId);
        verifyTotalPages(book, totalPages);
        LibraryItem item = LibraryItem.create(memberId, bookId, status,
                effectiveTotalPages(book, totalPages), now());
        return LibraryItemResponse.from(libraryItemRepository.save(item), book);
    }

    @Transactional
    public LibraryItemResponse addByIsbn13(long memberId, String isbn13, ReadingStatus status,
                                           Integer totalPages) {
        Book book = bookRepository.findByIsbn13(isbn13).orElseThrow(BookNotFoundException::new);
        return add(memberId, book.getId(), status, totalPages);
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatingsByIsbn13(long memberId, String isbn13,
                                                           BigDecimal criterion) {
        Book book = bookRepository.findByIsbn13(isbn13).orElseThrow(BookNotFoundException::new);
        return compareRatings(memberId, book.getId(), criterion);
    }

    @Transactional
    public LibraryItemResponse update(long memberId, long bookId, ReadingStatus status,
                                      Integer currentPage, Integer totalPages) {
        if ((status == null) == (currentPage == null)) {
            throw new IllegalArgumentException("Exactly one of status or current page is required");
        }
        Book book = getBook(bookId);
        verifyTotalPages(book, totalPages);
        LibraryItem item = getItem(memberId, bookId);
        Integer effectiveTotalPages = effectiveTotalPages(book, totalPages);
        if (status != null) {
            item.changeStatus(status, effectiveTotalPages, now());
        } else {
            item.changeCurrentPage(currentPage, effectiveTotalPages, now());
        }
        return LibraryItemResponse.from(item, book);
    }

    @Transactional
    public void delete(long memberId, long bookId) {
        getBook(bookId);
        libraryItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .ifPresent(libraryItemRepository::delete);
    }

    @Transactional
    public void bulkDelete(long memberId, Collection<Long> bookIds) {
        List<LibraryItem> items = requireAllLibraryItems(memberId, bookIds);
        libraryItemRepository.deleteAll(items);
    }

    @Transactional
    public void bulkChangeStatus(long memberId, Collection<Long> bookIds, ReadingStatus status) {
        List<LibraryItem> items = requireAllLibraryItems(memberId, bookIds);
        Instant now = now();
        items.forEach(item -> item.changeStatus(status, null, now));
    }

    @Transactional
    public LibraryItemResponse rate(long memberId, long bookId, BigDecimal rating) {
        Book book = getBook(bookId);
        LibraryItem item = libraryItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseGet(() -> libraryItemRepository.save(
                        LibraryItem.create(memberId, bookId, ReadingStatus.WANT_TO_READ, null, now())));
        item.rate(rating, now());
        return LibraryItemResponse.from(item, book);
    }

    @Transactional
    public void removeRating(long memberId, long bookId) {
        getBook(bookId);
        libraryItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .ifPresent(LibraryItem::removeRating);
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatings(long memberId, long currentBookId,
                                                   BigDecimal criterion) {
        Book currentBook = getBook(currentBookId);
        RatingComparisonBookResponse lower = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                        memberId, currentBookId, criterion)
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        RatingComparisonBookResponse higher = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                        memberId, currentBookId, criterion)
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        return new RatingComparisonResponse(lower,
                new RatingComparisonBookResponse(currentBookId, currentBook.getIsbn13(),
                        currentBook.getTitle(), currentBook.getCoverImageUrl(), currentBook.getAuthors(),
                        criterion), higher);
    }

    private LibraryItem getItem(long memberId, long bookId) {
        return libraryItemRepository.findByMemberIdAndBookId(memberId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND));
    }

    private List<LibraryItem> requireAllLibraryItems(long memberId, Collection<Long> bookIds) {
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId, bookIds);
        if (items.size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
        return items;
    }

    private Instant now() {
        return clock.instant();
    }

    private Book getBook(long bookId) {
        return bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
    }

    private void verifyTotalPages(Book book, Integer totalPages) {
        if (totalPages != null && book.getTotalPages() != null
                && !totalPages.equals(book.getTotalPages())) {
            throw new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT);
        }
    }

    private Integer effectiveTotalPages(Book book, Integer totalPages) {
        return totalPages != null ? totalPages : book.getTotalPages();
    }

    private Sort sortOf(LibrarySort sort) {
        LibrarySort effectiveSort = sort == null ? LibrarySort.RECENT : sort;
        return switch (effectiveSort) {
            case RECENT -> Sort.by(Sort.Order.desc("readingUpdatedAt"), Sort.Order.desc("bookId"));
            case OLDEST -> Sort.by(Sort.Order.asc("readingUpdatedAt"), Sort.Order.asc("bookId"));
            case RATING -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("ratingUpdatedAt"),
                    Sort.Order.desc("bookId"));
            // Book and review data are combined after Book/Review integration; stable local fallback.
            case COMMENT, TITLE -> Sort.by(Sort.Order.desc("readingUpdatedAt"), Sort.Order.desc("bookId"));
        };
    }
}
