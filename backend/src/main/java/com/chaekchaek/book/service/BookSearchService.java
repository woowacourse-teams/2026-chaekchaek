package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.client.dto.AladinSearchResponse;
import com.chaekchaek.book.dto.BookItem;
import com.chaekchaek.book.dto.BookSearchResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final AladinBookClient bookClient;

    public BookSearchResponse search(String query, int page) {
        AladinSearchResponse source = bookClient.searchBooks(query, page);

        List<BookItem> items = source.items()
                .stream()
                .map(this::toBookItem)
                .toList();
        return new BookSearchResponse(
                source.totalResults(),
                source.startIndex(),
                source.itemsPerPage(),
                source.hasNextPage(),
                items
        );
    }

    private BookItem toBookItem(AladinBookItem source) {
        AladinContributorParser.Contributors contributors =
                AladinContributorParser.parse(source.author());

        return new BookItem(
                source.title(),
                source.cover(),
                contributors.authors(),
                contributors.translators(),
                source.pubDate(),
                source.isbn13(),
                source.categoryName(),
                source.publisher()
        );
    }
}
