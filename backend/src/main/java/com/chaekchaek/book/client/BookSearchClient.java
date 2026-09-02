package com.chaekchaek.book.client;

import com.chaekchaek.book.domain.Isbn13;

public interface BookSearchClient {

    BookSearchResult search(String query, int page);

    BookDetailItem findBookByIsbn13(Isbn13 isbn13);
}
