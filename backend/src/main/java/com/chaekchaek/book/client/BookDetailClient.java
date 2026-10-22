package com.chaekchaek.book.client;

import com.chaekchaek.book.domain.Isbn13;

public interface BookDetailClient {

    BookDetailItem findBookByIsbn13(Isbn13 isbn13);
}
