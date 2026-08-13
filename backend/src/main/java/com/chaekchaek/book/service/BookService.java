package com.chaekchaek.book.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookResolver bookResolver;
    private final BookRepository bookRepository;
    private final BookDetailAssembler bookDetailAssembler;

    public com.chaekchaek.book.dto.BookDetailResponse resolve(String isbn13) {
        return bookDetailAssembler.assemble(bookResolver.resolve(isbn13));
    }

    @Transactional(readOnly = true)
    public com.chaekchaek.book.dto.BookDetailResponse getDetail(long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(BookNotFoundException::new);
        return bookDetailAssembler.assemble(book);
    }
}
