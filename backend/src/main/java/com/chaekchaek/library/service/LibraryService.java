package com.chaekchaek.library.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.book.service.BookResolver;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.LibrarySort;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.dto.LibraryListResponse;
import com.chaekchaek.library.dto.LibraryResponse;
import com.chaekchaek.library.dto.PublicLibraryListResponse;
import com.chaekchaek.library.dto.RatingComparisonBookResponse;
import com.chaekchaek.library.dto.RatingComparisonResponse;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);
    private static final int PAGE_SIZE = 10;
    private static final int LIBRARY_BOOK_LIMIT = 30;

    private final LibraryRepository libraryRepository;
    private final LibraryItemRepository libraryItemRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final BookRepository bookRepository;
    private final BookResolver bookResolver;
    private final BookCommentCountReader commentCountReader;
    private final MemberRepository memberRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final TransactionTemplate readTransaction;

    public LibraryService(
            LibraryRepository libraryRepository,
            LibraryItemRepository libraryItemRepository,
            LibraryBookRepository libraryBookRepository,
            BookRepository bookRepository,
            BookResolver bookResolver,
            BookCommentCountReader commentCountReader,
            MemberRepository memberRepository,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.libraryRepository = libraryRepository;
        this.libraryItemRepository = libraryItemRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.bookRepository = bookRepository;
        this.bookResolver = bookResolver;
        this.commentCountReader = commentCountReader;
        this.memberRepository = memberRepository;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
    }

    @Transactional
    public LibraryResponse create(long memberId, String name) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (member.getNickname() == null) {
            throw new BusinessException(ErrorCode.NICKNAME_REQUIRED);
        }
        Library library = Library.create(memberId, name, now());
        long displayOrder = libraryRepository.findMaxDisplayOrderByMemberId(memberId) + 1;
        library.changeDisplayOrder(displayOrder);
        Library saved = libraryRepository.create(library);
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", "library_created")
                .addKeyValue("memberId", memberId)
                .addKeyValue("libraryId", saved.getId())
                .log("Library created");
        return LibraryResponse.from(saved);
    }

    @Transactional
    public void rename(long memberId, long libraryId, String name) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        if (!library.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!library.rename(name, now())) {
            return;
        }
        libraryRepository.rename(library);
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", "library_renamed")
                .addKeyValue("memberId", memberId)
                .addKeyValue("libraryId", libraryId)
                .log("Library renamed");
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LibraryListResponse getLibrary(
            long memberId,
            int page,
            ReadingStatus status,
            LibrarySort sort
    ) {
        if (page < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<LibraryItem> allItems = defaultLibraryItems(memberId);
        List<LibraryItem> filteredItems = allItems
                .stream()
                .filter(item -> status == null || item.getStatus() == status)
                .toList();
        Map<Long, Book> books = booksById(filteredItems);
        Map<Long, Long> commentCounts = commentCountReader.getCommentCounts(books.keySet());
        List<LibraryItem> sortedItems = filteredItems
                .stream()
                .sorted(comparator(sort, books, commentCounts))
                .toList();
        List<LibraryItem> pageItems = pageOf(sortedItems, page);
        Integer nextPage = null;
        if ((long) page * PAGE_SIZE < sortedItems.size()) {
            nextPage = page + 1;
        }
        return new LibraryListResponse(
                allItems.size(),
                filteredItems.size(),
                nextPage,
                pageItems
                        .stream()
                        .map(item -> response(item, books.get(item.getBookId()), commentCounts))
                        .toList()
        );
    }

    private List<LibraryItem> defaultLibraryItems(long memberId) {
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(memberId).orElse(null);
        if (library == null) {
            return List.of();
        }
        List<Long> bookIds = libraryBookRepository.findAllByLibraryId(library.getId())
                .stream()
                .map(LibraryBook::getBookId)
                .toList();
        if (bookIds.isEmpty()) {
            return List.of();
        }
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId, bookIds);
        boolean missingReadingState = items
                .stream()
                .anyMatch(item -> !item.hasReadingState());
        if (items.size() != bookIds.size() || missingReadingState) {
            throw new IllegalStateException("Default library placements must have shared reading records");
        }
        return items;
    }

    private Map<Long, Book> booksById(Collection<LibraryItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        return bookRepository.findAllById(items.stream()
                .map(LibraryItem::getBookId)
                .toList())
                .stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));
    }

    private Comparator<LibraryItem> comparator(LibrarySort sort, Map<Long, Book> books, Map<Long, Long> commentCounts) {
        LibrarySort effectiveSort = sort == null ? LibrarySort.RECENT : sort;
        return switch (effectiveSort) {
            case RECENT -> Comparator.comparing(
                            LibraryItem::getReadingUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case OLDEST -> Comparator.comparing(
                            LibraryItem::getReadingUpdatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    )
                    .thenComparing(LibraryItem::getBookId);
            case COMMENT -> Comparator.comparingLong((LibraryItem item) ->
                            commentCounts.getOrDefault(item.getBookId(), 0L)).reversed()
                    .thenComparing(
                            LibraryItem::getReadingUpdatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case RATING -> Comparator.comparing(
                    LibraryItem::getRating,
                    Comparator.nullsLast(Comparator.reverseOrder())
            )
                    .thenComparing(
                    LibraryItem::getRatingUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            )
                    .thenComparing(LibraryItem::getBookId, Comparator.reverseOrder());
            case TITLE -> Comparator.comparing((LibraryItem item) -> books.get(item.getBookId()).getTitle())
                    .thenComparing(LibraryItem::getBookId);
        };
    }

    private List<LibraryItem> pageOf(List<LibraryItem> items, int page) {
        long start = (long) (page - 1) * PAGE_SIZE;
        if (start >= items.size()) {
            return List.of();
        }
        int startIndex = (int) start;
        return items.subList(startIndex, Math.min(startIndex + PAGE_SIZE, items.size()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PublicLibraryListResponse getPublicLibrary(
            long memberId,
            int page,
            ReadingStatus status,
            LibrarySort sort
    ) {
        Member member = memberRepository.findById(memberId)
                .filter(candidate -> candidate.getAccountStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        return PublicLibraryListResponse.from(member, getLibrary(memberId, page, status, sort));
    }

    @Transactional
    public LibraryItemResponse add(
            long memberId,
            long bookId,
            ReadingStatus status,
            Integer totalPages
    ) {
        lockMember(memberId);
        Book book = getBookForUpdate(bookId);
        Library library = requireDefaultLibrary(memberId);
        validateNewPlacement(library.getId(), bookId);
        rememberTotalPages(book, totalPages);
        LibraryItem item = readingItem(memberId, book, status);
        savePlacement(library.getId(), bookId);
        logItemEvent("library_item_added", "Library item added", memberId, bookId);
        return response(item, book, commentCountReader.getCommentCounts(List.of(bookId)));
    }

    private Library requireDefaultLibrary(long memberId) {
        return libraryRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NICKNAME_REQUIRED));
    }

    private void validateNewPlacement(long libraryId, long bookId) {
        if (libraryBookRepository.existsByLibraryIdAndBookId(libraryId, bookId)) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        }
        if (libraryBookRepository.countByLibraryId(libraryId) >= LIBRARY_BOOK_LIMIT) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED);
        }
    }

    private LibraryItem readingItem(long memberId, Book book, ReadingStatus status) {
        LibraryItem existing = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, book.getId())
                .orElse(null);
        if (existing == null) {
            return libraryItemRepository.saveAndFlush(
                    LibraryItem.create(memberId, book.getId(), status, book.getTotalPages(), now())
            );
        }
        if (!existing.hasReadingState()) {
            existing.startReading(status, book.getTotalPages(), now());
        }
        return existing;
    }

    private void savePlacement(long libraryId, long bookId) {
        long displayOrder = libraryBookRepository.findMaxDisplayOrderByLibraryId(libraryId) + 1;
        libraryBookRepository.create(LibraryBook.create(libraryId, bookId, displayOrder));
    }

    private void logItemEvent(String event, String message, long memberId, long bookId) {
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", event)
                .addKeyValue("memberId", memberId)
                .addKeyValue("bookId", bookId)
                .log(message);
    }

    private void logBulkEvent(String event, String message, long memberId, int itemCount) {
        log.atInfo()
                .addKeyValue("type", "event")
                .addKeyValue("event", event)
                .addKeyValue("memberId", memberId)
                .addKeyValue("itemCount", itemCount)
                .log(message);
    }

    public LibraryItemResponse addByIsbn13(
            long memberId,
            Isbn13 isbn13,
            ReadingStatus status,
            Integer totalPages
    ) {
        readTransaction.executeWithoutResult(transactionStatus -> requireDefaultLibrary(memberId));
        Book book = bookResolver.findOrCreate(isbn13);
        return Objects.requireNonNull(transactionTemplate.execute(transactionStatus ->
                add(memberId, book.getId(), status, totalPages)));
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatingsByIsbn13(long memberId, Isbn13 isbn13, BigDecimal criterion) {
        validateRating(criterion);
        Book currentBook = bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> bookResolver.lookup(isbn13));
        return comparison(memberId, currentBook, criterion);
    }

    @Transactional
    public LibraryItemResponse update(
            long memberId,
            long bookId,
            ReadingStatus status,
            Integer currentPage,
            Integer totalPages
    ) {
        if ((status == null) == (currentPage == null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        lockMember(memberId);
        Book book = getBookForUpdate(bookId);
        LibraryItem item = getItemForUpdate(memberId, bookId);
        requireReadingState(item);
        rememberTotalPages(book, totalPages);
        if (currentPage != null && book.getTotalPages() == null) {
            throw new BusinessException(ErrorCode.INVALID_READING_STATE);
        }
        if (status != null) {
            item.changeStatus(status, book.getTotalPages(), now());
        } else {
            item.changeCurrentPage(currentPage, book.getTotalPages(), now());
        }
        logItemEvent("library_item_updated", "Library item updated", memberId, bookId);
        return response(item, book, Map.of());
    }

    private void rememberTotalPages(Book book, Integer totalPages) {
        if (totalPages != null) {
            book.rememberTotalPages(totalPages);
        }
    }

    @Transactional
    public void delete(long memberId, long bookId) {
        lockMember(memberId);
        getBook(bookId);
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(memberId).orElse(null);
        if (library == null) {
            return;
        }
        libraryBookRepository.findByLibraryIdAndBookId(library.getId(), bookId)
                .ifPresent(placement -> removePlacements(memberId, List.of(placement)));
        logItemEvent("library_item_deleted", "Library item deleted", memberId, bookId);
    }

    @Transactional
    public void bulkDelete(long memberId, Collection<Long> bookIds) {
        validateBulkBookIds(bookIds);
        lockMember(memberId);
        Library library = libraryRepository.findByMemberIdAndIsDefaultTrue(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND));
        List<LibraryBook> placements = libraryBookRepository.findAllByLibraryIdAndBookIdIn(library.getId(), bookIds);
        if (placements.size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
        removePlacements(memberId, placements);
        logBulkEvent("library_items_deleted", "Library items deleted", memberId, bookIds.size());
    }

    private void removePlacements(long memberId, List<LibraryBook> placements) {
        List<Long> bookIds = placements
                .stream()
                .map(LibraryBook::getBookId)
                .toList();
        List<LibraryItem> records = requireAllReadingItemsForUpdate(memberId, bookIds);
        libraryBookRepository.deleteAll(placements);
        libraryBookRepository.flush();
        Set<Long> remainingBookIds = new HashSet<>(
                libraryBookRepository.findPlacedBookIdsByMemberIdAndBookIdIn(memberId, bookIds)
        );
        records
                .stream()
                .filter(record -> !remainingBookIds.contains(record.getBookId()))
                .forEach(LibraryItem::clearReadingState);
    }

    @Transactional
    public void bulkChangeStatus(long memberId, Collection<Long> bookIds, ReadingStatus status) {
        validateBulkBookIds(bookIds);
        lockMember(memberId);
        if (status == ReadingStatus.FINISHED) {
            validateAllLibraryItemsExist(memberId, bookIds);
            Map<Long, Book> books = booksByIdForUpdate(bookIds);
            List<LibraryItem> items = requireAllReadingItemsForUpdate(memberId, bookIds);
            if (books.values()
                    .stream()
                    .anyMatch(book -> book.getTotalPages() == null)) {
                throw new BusinessException(ErrorCode.INVALID_READING_STATE);
            }
            items.forEach(item -> item.changeStatus(
                    status,
                    books.get(item.getBookId()).getTotalPages(),
                    now()
            ));
            logBulkEvent("library_items_status_changed", "Library items status changed", memberId, items.size());
            return;
        }
        List<LibraryItem> items = requireAllReadingItemsForUpdate(memberId, bookIds);
        Instant now = now();
        items.forEach(item -> item.changeStatus(status, null, now));
        logBulkEvent("library_items_status_changed", "Library items status changed", memberId, items.size());
    }

    private void validateBulkBookIds(Collection<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty() || bookIds.size() > PAGE_SIZE
                || bookIds.stream()
                        .anyMatch(bookId -> bookId == null)
                || new HashSet<>(bookIds).size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateAllLibraryItemsExist(long memberId, Collection<Long> bookIds) {
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId, bookIds);
        if (items.size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
        items.forEach(this::requireReadingState);
    }

    private Map<Long, Book> booksByIdForUpdate(Collection<Long> bookIds) {
        return bookIds.stream()
                .sorted()
                .collect(Collectors.toMap(Function.identity(), this::getBookForUpdate));
    }

    private Book getBookForUpdate(long bookId) {
        return bookRepository.findByIdForUpdate(bookId).orElseThrow(BookNotFoundException::new);
    }

    private List<LibraryItem> requireAllReadingItemsForUpdate(long memberId, Collection<Long> bookIds) {
        List<LibraryItem> items = libraryItemRepository.findAllByMemberIdAndBookIdInForUpdate(memberId, bookIds);
        if (items.size() != bookIds.size()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
        items.forEach(this::requireReadingState);
        return items;
    }

    public LibraryItemResponse rateByIsbn13(long memberId, Isbn13 isbn13, BigDecimal rating) {
        validateRating(rating);
        Book book = bookResolver.findOrCreate(isbn13);
        return Objects.requireNonNull(transactionTemplate.execute(transactionStatus ->
                rate(memberId, book.getId(), rating)));
    }

    @Transactional
    public LibraryItemResponse rate(long memberId, long bookId, BigDecimal rating) {
        validateRating(rating);
        lockMember(memberId);
        Book book = getBook(bookId);
        Instant now = now();
        LibraryItem item = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId).orElse(null);
        if (item == null) {
            item = libraryItemRepository.saveAndFlush(
                    LibraryItem.createForRating(memberId, bookId, rating, now)
            );
        } else {
            item.rate(rating, now);
        }
        logItemEvent("library_item_rated", "Library item rated", memberId, bookId);
        return response(item, book, Map.of());
    }

    private void lockMember(long memberId) {
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireReadingState(LibraryItem item) {
        if (!item.hasReadingState()) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND);
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private LibraryItem getItemForUpdate(long memberId, long bookId) {
        return libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_ITEM_NOT_FOUND));
    }

    private LibraryItemResponse response(LibraryItem item, Book book, Map<Long, Long> commentCounts) {
        return LibraryItemResponse.from(item, book, commentCounts.getOrDefault(item.getBookId(), 0L));
    }

    @Transactional
    public void removeRating(long memberId, long bookId) {
        lockMember(memberId);
        getBook(bookId);
        libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .ifPresent(item -> {
                    item.removeRating();
                    logItemEvent("library_item_rating_removed", "Library item rating removed", memberId, bookId);
                });
    }

    @Transactional(readOnly = true)
    public RatingComparisonResponse compareRatings(long memberId, long currentBookId, BigDecimal criterion) {
        validateRating(criterion);
        return comparison(memberId, getBook(currentBookId), criterion);
    }

    private void validateRating(BigDecimal rating) {
        if (rating == null || rating.compareTo(new BigDecimal("0.1")) < 0
                || rating.compareTo(new BigDecimal("5.0")) > 0 || rating.scale() > 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private RatingComparisonResponse comparison(long memberId, Book currentBook, BigDecimal criterion) {
        long excludedBookId = currentBook.getId() == null ? -1L : currentBook.getId();
        RatingComparisonBookResponse lower = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingLessThanOrderByRatingDescRatingUpdatedAtDescBookIdDesc(
                memberId,
                excludedBookId,
                criterion
        )
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        RatingComparisonBookResponse current = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingOrderByRatingUpdatedAtDescBookIdDesc(
                memberId,
                excludedBookId,
                criterion
        )
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        RatingComparisonBookResponse higher = libraryItemRepository
                .findFirstByMemberIdAndBookIdNotAndRatingGreaterThanOrderByRatingAscRatingUpdatedAtDescBookIdDesc(
                memberId,
                excludedBookId,
                criterion
        )
                .map(item -> RatingComparisonBookResponse.from(item, getBook(item.getBookId())))
                .orElse(null);
        return new RatingComparisonResponse(
                lower,
                current,
                higher
        );
    }

    private Book getBook(long bookId) {
        return bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
    }
}
