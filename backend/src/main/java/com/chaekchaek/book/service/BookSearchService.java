package com.chaekchaek.book.service;

import com.chaekchaek.book.client.BookSearchClient;
import com.chaekchaek.book.client.BookSearchItem;
import com.chaekchaek.book.client.BookSearchResult;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.domain.BookSearchSort;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookSearchService {

    private final BookSearchClient bookClient;
    private final BookRepository bookRepository;
    private final BookActivityCountReader activityCountReader;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final LibraryItemRepository libraryItemRepository;

    public BookSearchResponse search(String query, int page) {
        return search(query, page, BookSearchSort.LATEST);
    }

    public BookSearchResponse search(String query, int page, BookSearchSort sort) {
        BookSearchResult source = bookClient.search(query, page);
        List<BookSearchItem> searchedBooks = source.items();
        List<Isbn13> searchResultIsbn13s = searchedBooks.stream()
                .map(BookSearchItem::isbn13)
                .map(Isbn13::new)
                .toList();

        List<Book> registeredBooks = bookRepository.findAllByIsbn13In(searchResultIsbn13s);
        Map<Isbn13, Book> registeredBooksByIsbn13 = registeredBooks.stream()
                .collect(Collectors.toMap(Book::getIsbn13, Function.identity()));

        Map<Long, ActivityCounts> activityCountsByBookId = activityCountReader.getActivityCounts(
                registeredBooks.stream()
                        .map(Book::getId)
                        .toList()
        );
        OptionalLong memberId = currentMemberIdProvider.findCurrentMemberId();
        Set<Long> libraryBookIds = findLibraryBookIds(memberId, registeredBooks);

        List<BookItem> items = new ArrayList<>(searchedBooks.size());
        for (int index = 0; index < searchedBooks.size(); index++) {
            BookSearchItem searchedBook = searchedBooks.get(index);
            Book registeredBook = registeredBooksByIsbn13.get(searchResultIsbn13s.get(index));
            ActivityCounts activityCounts = registeredBook == null
                    ? null
                    : activityCountsByBookId.getOrDefault(registeredBook.getId(), ActivityCounts.ZERO);

            items.add(toBookItem(searchedBook, registeredBook, activityCounts, memberId, libraryBookIds));
        }
        items.sort(comparator(sort));

        return new BookSearchResponse(
                source.totalCount(),
                source.nextPage(),
                List.copyOf(items)
        );
    }

    private Set<Long> findLibraryBookIds(OptionalLong memberId, Collection<Book> registeredBooks) {
        if (memberId.isEmpty() || registeredBooks.isEmpty()) {
            return Set.of();
        }
        List<Long> bookIds = registeredBooks.stream().map(Book::getId).toList();
        return libraryItemRepository.findAllByMemberIdAndBookIdIn(memberId.getAsLong(), bookIds)
                .stream()
                .map(LibraryItem::getBookId)
                .collect(Collectors.toSet());
    }

    private Comparator<BookItem> comparator(BookSearchSort sort) {
        BookSearchSort effectiveSort = sort == null ? BookSearchSort.LATEST : sort;
        return switch (effectiveSort) {
            case TITLE_ASC -> Comparator.comparing(BookItem::title,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case TITLE_DESC -> Comparator.comparing(BookItem::title,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case OLDEST -> Comparator.comparing(BookItem::publishedDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case LATEST -> Comparator.comparing(BookItem::publishedDate,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case REVIEW -> Comparator.comparing(BookItem::reviewCount,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            case COMMENT -> Comparator.comparing(this::totalCount,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    private BookItem toBookItem(
            BookSearchItem source,
            Book registeredBook,
            ActivityCounts activityCounts,
            OptionalLong memberId,
            Set<Long> libraryBookIds
    ) {
        return new BookItem(
                registeredBook == null ? null : registeredBook.getId(),
                source.title(),
                source.coverImageUrl(),
                source.authors(),
                source.translators(),
                source.publishedDate() == null
                        ? null
                        : source.publishedDate().toString(),
                source.isbn13(),
                source.category(),
                source.publisher(),
                activityCounts == null
                        ? null
                        : Math.toIntExact(activityCounts.reviewCount()),
                activityCounts == null
                        ? null
                        : Math.toIntExact(activityCounts.replyCount()),
                isRegisteredInMyLibrary(registeredBook, memberId, libraryBookIds)
        );
    }

    private Boolean isRegisteredInMyLibrary(Book registeredBook, OptionalLong memberId, Set<Long> libraryBookIds) {
        if (memberId.isEmpty()) {
            return null;
        }
        return registeredBook != null && libraryBookIds.contains(registeredBook.getId());
    }

    private Long totalCount(BookItem item) {
        if (item.reviewCount() == null || item.replyCount() == null) {
            return null;
        }
        return (long) item.reviewCount() + item.replyCount();
    }
}
