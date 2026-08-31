package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.BookSearchSort;
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

    public static final ActivityCounts EMPTY_ACTIVITY_COUNTS = new ActivityCounts(0L, 0L);

    private final AladinBookClient bookClient;
    private final BookRepository bookRepository;
    private final BookActivityCountReader activityCountReader;
    private final CurrentMemberIdProvider currentMemberIdProvider;
    private final LibraryItemRepository libraryItemRepository;

    public BookSearchResponse search(String query, int page) {
        return search(query, page, BookSearchSort.LATEST);
    }

    public BookSearchResponse search(String query, int page, BookSearchSort sort) {
        AladinSearchResponse source = bookClient.searchBooks(query, page);
        List<AladinBookItem> searchedBooks = source.items();
        List<String> searchResultIsbn13s = searchedBooks.stream()
                .map(AladinBookItem::isbn13)
                .toList();

        List<Book> registeredBooks = bookRepository.findAllByIsbn13In(searchResultIsbn13s);
        Map<String, Book> registeredBooksByIsbn13 = registeredBooks.stream()
                .collect(Collectors.toMap(Book::getIsbn13, Function.identity()));

        Map<Long, ActivityCounts> activityCountsByBookId = activityCountReader.getActivityCounts(
                registeredBooks.stream()
                        .map(Book::getId)
                        .toList()
        );
        OptionalLong memberId = currentMemberIdProvider.findCurrentMemberId();
        Set<Long> libraryBookIds = findLibraryBookIds(memberId, registeredBooks);

        List<BookItem> items = new ArrayList<>(searchedBooks.size());
        for (AladinBookItem searchedBook : searchedBooks) {
            Book registeredBook = registeredBooksByIsbn13.get(searchedBook.isbn13());
            ActivityCounts activityCounts = registeredBook == null
                    ? null
                    : activityCountsByBookId.getOrDefault(registeredBook.getId(), EMPTY_ACTIVITY_COUNTS);

            items.add(toBookItem(searchedBook, registeredBook, activityCounts, memberId, libraryBookIds));
        }
        items.sort(comparator(sort));

        Integer nextPage = source.hasNextPage()
                ? source.startIndex() + 1
                : null;
        return new BookSearchResponse(
                source.totalResults(),
                nextPage,
                items
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
        if (effectiveSort == BookSearchSort.COMMENT) {
            return Comparator.comparing(
                    this::totalCount,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }
        return Comparator.comparing(
                BookItem::publishedDate,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private BookItem toBookItem(
            AladinBookItem source,
            Book registeredBook,
            ActivityCounts activityCounts,
            OptionalLong memberId,
            Set<Long> libraryBookIds
    ) {
        AladinContributorParser.Contributors contributors =
                AladinContributorParser.parse(source.author());

        return new BookItem(
                registeredBook == null ? null : registeredBook.getId(),
                source.title(),
                source.cover(),
                contributors.authors(),
                contributors.translators(),
                source.pubDate(),
                source.isbn13(),
                source.categoryName(),
                source.publisher(),
                activityCounts == null ? null : Math.toIntExact(activityCounts.reviewCount()),
                activityCounts == null ? null : Math.toIntExact(activityCounts.replyCount()),
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
