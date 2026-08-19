package com.chaekchaek.book.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookResolver bookResolver;
    private final BookDetailAssembler bookDetailAssembler;

    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(String isbn13) {
        Book book = bookResolver.lookup(isbn13);
        return bookDetailAssembler.assemble(book);
    }
}
