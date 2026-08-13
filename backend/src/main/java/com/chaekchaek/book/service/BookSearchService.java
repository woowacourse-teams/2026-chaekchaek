package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.service.BookCommentCountReader;
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
    private final BookCommentCountReader commentCountReader;

    public BookSearchResponse search(String query, int page) {
        AladinSearchResponse source = bookClient.searchBooks(query, page);

        Map<String, Book> registeredBooks = bookRepository.findAllByIsbn13In(
                        source.items().stream().map(AladinBookItem::isbn13).toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Book::getIsbn13, Function.identity()));
        Map<Long, Long> commentCounts = commentCountReader.getCommentCounts(
                registeredBooks.values().stream().map(Book::getId).toList());
        List<BookItem> items = source.items()
                .stream()
                .map(item -> toBookItem(item, registeredBooks.get(item.isbn13()), commentCounts))
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

    private BookItem toBookItem(
            AladinBookItem source,
            Book registeredBook,
            Map<Long, Long> commentCounts
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
                        commentCounts.getOrDefault(registeredBook.getId(), 0L))
        );
    }
}
