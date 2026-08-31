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

        Map<String, Book> registeredBooks = bookRepository.findAllByIsbn13In(
                        source.items().stream().map(AladinBookItem::isbn13).toList())
                .stream()
                .collect(Collectors.toMap(Book::getIsbn13, Function.identity()));
        Map<Long, ActivityCounts> activityCounts = activityCountReader.getActivityCounts(
                registeredBooks.values().stream().map(Book::getId).toList());
        OptionalLong memberId = currentMemberIdProvider.findCurrentMemberId();
        Set<Long> libraryBookIds = findLibraryBookIds(memberId, registeredBooks.values());
        List<BookItem> items = source.items()
                .stream()
                .map(item -> toBookItem(
                        item,
                        registeredBooks.get(item.isbn13()),
                        activityCounts,
                        memberId,
                        libraryBookIds
                ))
                .sorted(comparator(sort))
                .toList();
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
        return switch (effectiveSort) {
            case TITLE_ASC -> Comparator.comparing(BookItem::title, Comparator.nullsLast(Comparator.naturalOrder()));
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
            AladinBookItem source,
            Book registeredBook,
            Map<Long, ActivityCounts> activityCounts,
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
                registeredBook == null ? null : Math.toIntExact(
                        activityCounts.getOrDefault(registeredBook.getId(), new ActivityCounts(0L, 0L))
                                .reviewCount()),
                registeredBook == null ? null : Math.toIntExact(
                        activityCounts.getOrDefault(registeredBook.getId(), new ActivityCounts(0L, 0L))
                                .replyCount()),
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
