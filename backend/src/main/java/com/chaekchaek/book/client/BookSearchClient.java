package com.chaekchaek.book.client;

public interface BookSearchClient {

    BookSearchResult search(String query, int page);
}
