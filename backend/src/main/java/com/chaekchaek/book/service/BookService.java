package com.chaekchaek.book.service;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookDetailAssembler bookDetailAssembler;

    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(long bookId) {
        return bookDetailAssembler.assemble(getDetailBook(bookId));
    }

    private Book getDetailBook(long bookId) {
        return bookRepository.findDetailById(bookId).orElseThrow(BookNotFoundException::new);
    }
}
