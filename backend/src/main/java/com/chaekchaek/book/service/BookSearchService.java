package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.BookSearchSort;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final AladinBookClient bookClient;
    private final BookRepository bookRepository;
    private final BookActivityCountReader activityCountReader;

    public BookSearchResponse search(String query, int page) {
        return search(query, page, BookSearchSort.LATEST);
    }

    public BookSearchResponse search(String query, int page, BookSearchSort sort) {
        AladinSearchResponse source = bookClient.searchBooks(query, page);

        Map<String, Book> registeredBooks = bookRepository.findAllByIsbn13In(
                        source.items().stream().map(AladinBookItem::isbn13).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Book::getIsbn13, Function.identity()));
        Map<Long, ActivityCounts> activityCounts = activityCountReader.getActivityCounts(
                registeredBooks.values().stream().map(Book::getId).toList());
        List<BookItem> items = source.items()
                .stream()
                .map(item -> toBookItem(item, registeredBooks.get(item.isbn13()), activityCounts))
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
            Map<Long, ActivityCounts> activityCounts
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
                                .replyCount())
        );
    }

    private Long totalCount(BookItem item) {
        if (item.reviewCount() == null || item.replyCount() == null) {
            return null;
        }
        return (long) item.reviewCount() + item.replyCount();
    }
}
