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
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.dto.LibraryItemResponse;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LibraryBookService {

    private static final int LIBRARY_BOOK_LIMIT = 30;

    private final LibraryRepository libraryRepository;
    private final LibraryBookRepository libraryBookRepository;
    private final LibraryItemRepository libraryItemRepository;
    private final BookRepository bookRepository;
    private final BookResolver bookResolver;
    private final BookCommentCountReader commentCountReader;
    private final MemberRepository memberRepository;
    private final Clock clock;
    private final TransactionTemplate readTransaction;
    private final TransactionTemplate writeTransaction;

    public LibraryBookService(
            LibraryRepository libraryRepository,
            LibraryBookRepository libraryBookRepository,
            LibraryItemRepository libraryItemRepository,
            BookRepository bookRepository,
            BookResolver bookResolver,
            BookCommentCountReader commentCountReader,
            MemberRepository memberRepository,
            Clock clock,
            PlatformTransactionManager transactionManager
    ) {
        this.libraryRepository = libraryRepository;
        this.libraryBookRepository = libraryBookRepository;
        this.libraryItemRepository = libraryItemRepository;
        this.bookRepository = bookRepository;
        this.bookResolver = bookResolver;
        this.commentCountReader = commentCountReader;
        this.memberRepository = memberRepository;
        this.clock = clock;
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setReadOnly(true);
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public LibraryItemResponse addByIsbn13(
            long memberId,
            long libraryId,
            Isbn13 isbn13,
            ReadingStatus status,
            Integer totalPages
    ) {
        readTransaction.executeWithoutResult(transactionStatus -> requireOwnedLibrary(memberId, libraryId));
        Book resolvedBook = bookResolver.findOrCreate(isbn13);
        return Objects.requireNonNull(writeTransaction.execute(transactionStatus ->
                add(memberId, libraryId, resolvedBook.getId(), status, totalPages)));
    }

    public void delete(long memberId, long libraryId, long bookId) {
        writeTransaction.executeWithoutResult(transactionStatus -> deletePlacement(memberId, libraryId, bookId));
    }

    private void deletePlacement(long memberId, long libraryId, long bookId) {
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        requireOwnedLibrary(memberId, libraryId);

        libraryBookRepository.findByLibraryIdAndBookId(libraryId, bookId).ifPresent(placement -> {
            libraryBookRepository.delete(placement);
            libraryBookRepository.flush();
            if (libraryBookRepository.countByMemberIdAndBookId(memberId, bookId) == 0) {
                LibraryItem item = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                        .orElseThrow(() -> new IllegalStateException(
                                "A placed book must have a shared library item"
                        ));
                item.clearReadingState();
            }
        });
    }

    private LibraryItemResponse add(
            long memberId,
            long libraryId,
            long bookId,
            ReadingStatus status,
            Integer totalPages
    ) {
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        requireOwnedLibrary(memberId, libraryId);

        Book book = bookRepository.findByIdForUpdate(bookId).orElseThrow(BookNotFoundException::new);
        if (libraryBookRepository.existsByLibraryIdAndBookId(libraryId, bookId)) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_ALREADY_EXISTS);
        }
        if (libraryBookRepository.countByLibraryId(libraryId) >= LIBRARY_BOOK_LIMIT) {
            throw new BusinessException(ErrorCode.LIBRARY_ITEM_LIMIT_EXCEEDED);
        }
        book.rememberTotalPages(totalPages);

        Instant now = clock.instant();
        LibraryItem item = libraryItemRepository.findByMemberIdAndBookIdForUpdate(memberId, bookId)
                .map(existing -> startReadingIfNeeded(existing, status, book.getTotalPages(), now))
                .orElseGet(() -> createReadingItem(memberId, bookId, status, book.getTotalPages(), now));
        savePlacement(libraryId, bookId);
        long commentCount = commentCountReader.getCommentCounts(List.of(bookId)).getOrDefault(bookId, 0L);
        return LibraryItemResponse.from(item, book, commentCount);
    }

    private LibraryItem startReadingIfNeeded(
            LibraryItem item,
            ReadingStatus status,
            Integer totalPages,
            Instant now
    ) {
        if (!item.hasReadingState()) {
            item.startReading(status, totalPages, now);
        }
        return item;
    }

    private LibraryItem createReadingItem(
            long memberId,
            long bookId,
            ReadingStatus status,
            Integer totalPages,
            Instant now
    ) {
        return libraryItemRepository.saveAndFlush(
                LibraryItem.create(memberId, bookId, status, totalPages, now)
        );
    }

    private void savePlacement(long libraryId, long bookId) {
        long displayOrder = libraryBookRepository.findMaxDisplayOrderByLibraryId(libraryId) + 1;
        libraryBookRepository.create(LibraryBook.create(libraryId, bookId, displayOrder));
    }

    private void requireOwnedLibrary(long memberId, long libraryId) {
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIBRARY_NOT_FOUND));
        if (!library.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
