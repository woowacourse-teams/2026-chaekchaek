package com.chaekchaek.library.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.RatingComparisonBookResponse;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.dto.PublicLibraryListResponse;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LibraryService {

    private static final int PAGE_SIZE = 10;

    private final LibraryItemRepository libraryItemRepository;
    private final BookRepository bookRepository;
    private final BookResolver bookResolver;
    private final BookCommentCountReader commentCountReader;
    private final MemberRepository memberRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public LibraryService(
            LibraryItemRepository libraryItemRepository,
            BookRepository bookRepository,
            BookResolver bookResolver,
            BookCommentCountReader commentCountReader,
            MemberRepository memberRepository,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.libraryItemRepository = libraryItemRepository;
        this.bookRepository = bookRepository;
        this.bookResolver = bookResolver;
        this.commentCountReader = commentCountReader;
        this.memberRepository = memberRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public LibraryListResponse getLibrary(long memberId, int page, ReadingStatus status, LibrarySort sort) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<LibraryItem> filteredItems = status == null
                ? libraryItemRepository.findAllByMemberId(memberId)
                : libraryItemRepository.findAllByMemberIdAndStatus(memberId, status);
        Map<Long, Book> books = booksById(filteredItems);
        Map<Long, Long> commentCounts = commentCountReader.getCommentCounts(books.keySet());
        List<LibraryItem> sortedItems = filteredItems.stream()
                .sorted(comparator(sort, books, commentCounts))
                .toList();
        List<LibraryItem> pageItems = pageOf(sortedItems, page);
        Integer nextPage = page * PAGE_SIZE < sortedItems.size() ? page + 1 : null;
        return new LibraryListResponse(
                libraryItemRepository.countByMemberId(memberId),
                filteredItems.size(),
                nextPage,
                pageItems.stream()
                        .map(item -> response(item, books.get(item.getBookId()), commentCounts))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public PublicLibraryListResponse getPublicLibrary(long memberId, int page, ReadingStatus status,
                                                      LibrarySort sort) {
        if (memberRepository.findById(memberId)
                .filter(member -> member.getAccountStatus() == AccountStatus.ACTIVE)
                .isEmpty()) {
            throw new BusinessException(ErrorCode.LIBRARY_NOT_FOUND);
        }
        return PublicLibraryListResponse.from(getLibrary(memberId, page, status, sort));
    }

    @Transactional
    public LibraryItemResponse add(long memberId, long bookId, ReadingStatus status, Integer totalPages) {
        Book book = getBookForUpdate(bookId);
        rememberTotalPages(book, totalPages);
        if (libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId).isPresent()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        }
        return saveNewItem(memberId, book, status);
    }

    public LibraryItemResponse addByIsbn13(long memberId, Isbn13 isbn13, ReadingStatus status,
                                           Integer totalPages) {
        Book book = bookResolver.findOrCreate(isbn13);
        return transactionTemplate.execute(statusTemplate ->
                add(memberId, book.getId(), status, totalPages));
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatingsByIsbn13(long memberId, Isbn13 isbn13,
                                                           BigDecimal criterion) {
        validateRating(criterion);
        Book currentBook = bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> bookResolver.lookup(isbn13));
        return comparison(memberId, currentBook, criterion);
    }

    @Transactional
    public LibraryItemResponse update(long memberId, long bookId, ReadingStatus status,
                                      Integer currentPage, Integer totalPages) {
        if ((status == null) == (currentPage == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Book book = getBookForUpdate(bookId);
        LibraryItem item = getItemForUpdate(memberId, bookId);
        rememberTotalPages(book, totalPages);
        if (currentPage != null && book.getTotalPages() == null) {
            throw new BusinessException(ErrorCode.INVALID_READING_STATE);
        }
        if (status != null) {
            item.changeStatus(status, book.getTotalPages(), now());
        } else {
            item.changeCurrentPage(currentPage, book.getTotalPages(), now());
        }
        return response(item, book, Map.of());
    }

    @Transactional
    public void delete(long memberId, long bookId) {
        getBook(bookId);
        libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .ifPresent(libraryItemRepository::delete);
    }

    @Transactional
    public void bulkDelete(long memberId, Collection<Long> bookIds) {
        validateBulkBookIds(bookIds);
        libraryItemRepository.deleteAll(requireAllLibraryItemsForUpdate(memberId, bookIds));
    }

    @Transactional
    public void bulkChangeStatus(long memberId, Collection<Long> bookIds, ReadingStatus status) {
        validateBulkBookIds(bookIds);
        if (status == ReadingStatus.FINISHED) {
            validateAllLibraryItemsExist(memberId, bookIds);
            Map<Long, Book> books = booksByIdForUpdate(bookIds);
            List<LibraryItem> items = requireAllLibraryItemsForUpdate(memberId, bookIds);
            if (books.values().stream().anyMatch(book -> book.getTotalPages() == null)) {
                throw new BusinessException(ErrorCode.INVALID_READING_STATE);
            }
            items.forEach(item -> item.changeStatus(status,
                    books.get(item.getBookId()).getTotalPages(), now()));
            return;
        }
        List<LibraryItem> items = requireAllLibraryItemsForUpdate(memberId, bookIds);
        Instant now = now();
        items.forEach(item -> item.changeStatus(status, null, now));
    }

    @Transactional
    public LibraryItemResponse rate(long memberId, long bookId, BigDecimal rating) {
        validateRating(rating);
        Book book = getBook(bookId);
        LibraryItem item = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .orElseGet(() -> createForRating(memberId, book));
        item.rate(rating, now());
        return response(item, book, Map.of());
    }

    @Transactional
    public void removeRating(long memberId, long bookId) {
        getBook(bookId);
        libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .ifPresent(LibraryItem::removeRating);
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatings(long memberId, long currentBookId,
                                                   BigDecimal criterion) {
        validateRating(criterion);
        return comparison(memberId, getBook(currentBookId), criterion);
    }

    private RatingComparisonResponse comparison(long memberId, Book currentBook, BigDecimal criterion) {
        long excludedBookId = currentBook.getId() == null ? -1L : currentBook.getId();
        RatingComparisonBookResponse lower = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                        memberId, excludedBookId, criterion)
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        RatingComparisonBookResponse current = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                        memberId, excludedBookId, criterion)
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        RatingComparisonBookResponse higher = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                        memberId, excludedBookId, criterion)
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        return new RatingComparisonResponse(lower, current, higher);
    }

    private LibraryItemResponse saveNewItem(long memberId, Book book, ReadingStatus status) {
        try {
            LibraryItem item = LibraryItem.create(memberId, book.getId(), status,
                    book.getTotalPages(), now());
            return response(libraryItemRepository.saveAndFlush(item), book, Map.of());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        }
    }

    private LibraryItem createForRating(long memberId, Book book) {
        try {
            return libraryItemRepository.saveAndFlush(
                    LibraryItem.create(memberId, book.getId(), ReadingStatus.WANT_TO_READ, null, now()));
        } catch (DataIntegrityViolationException exception) {
            return getItemForUpdate(memberId, book.getId());
        }
    }

    private List<LibraryItem> requireAllLibraryItemsForUpdate(long memberId, Collection<Long> bookIds) {
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(memberId, bookIds);
        if (items.size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
        return items;
    }

    private LibraryItem getItemForUpdate(long memberId, long bookId) {
        return libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND));
    }

    private Map<Long, Book> booksById(Collection<LibraryItem> items) {
        return bookRepository.findAllById(items.stream().map(LibraryItem::getBookId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Book::getId, Function.identity()));
    }

    private Map<Long, Book> booksByIdForUpdate(Collection<Long> bookIds) {
        return bookIds.stream().sorted()
                .collect(java.util.stream.Collectors.toMap(Function.identity(), this::getBookForUpdate));
    }

    private void validateAllLibraryItemsExist(long memberId, Collection<Long> bookIds) {
        if (libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId, bookIds).size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
    }

    private List<LibraryItem> pageOf(List<LibraryItem> items, int page) {
        long start = (long) (page - 1) * PAGE_SIZE;
        if (start >= items.size()) {
            return List.of();
        }
        int startIndex = (int) start;
        return items.subList(startIndex, Math.min(startIndex + PAGE_SIZE, items.size()));
    }

    private Comparator<LibraryItem> comparator(LibrarySort sort, Map<Long, Book> books,
                                               Map<Long, Long> commentCounts) {
        LibrarySort effectiveSort = sort == null ? LibrarySort.RECENT : sort;
        return switch (effectiveSort) {
            case RECENT -> Comparator.comparing(LibraryItem::getReadingUpdatedAt).reversed()
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case OLDEST -> Comparator.comparing(LibraryItem::getReadingUpdatedAt)
                    .thenComparing(LibraryItem::getBookId);
            case COMMENT -> Comparator.comparingLong((LibraryItem item) ->
                            commentCounts.getOrDefault(item.getBookId(), 0L)).reversed()
                    .thenComparing(LibraryItem::getReadingUpdatedAt, Comparator.reverseOrder())
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case RATING -> Comparator.comparing(LibraryItem::getRating,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LibraryItem::getRatingUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case TITLE -> Comparator.comparing((LibraryItem item) -> books.get(item.getBookId()).getTitle())
                    .thenComparing(LibraryItem::getBookId);
        };
    }

    private LibraryItemResponse response(LibraryItem item, Book book, Map<Long, Long> commentCounts) {
        return LibraryItemResponse.from(item, book, commentCounts.getOrDefault(item.getBookId(), 0L));
    }

    private void rememberTotalPages(Book book, Integer totalPages) {
        if (totalPages != null) {
            book.rememberTotalPages(totalPages);
        }
    }

    private void validateBulkBookIds(Collection<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty() || bookIds.size() > PAGE_SIZE
                || bookIds.stream().anyMatch(bookId -> bookId == null)
                || new HashSet<>(bookIds).size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(new BigDecimal("0.1")) < 0
                || rating.compareTo(new BigDecimal("5.0")) > 0 || rating.scale() > 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private Book getBook(long bookId) {
        return bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
    }

    private Book getBookForUpdate(long bookId) {
        return bookRepository.findByIdForUpdate(bookId).orElseThrow(BookNotFoundException::new);
    }
}
