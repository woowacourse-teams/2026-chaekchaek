package com.chaekchaek.review.book;

import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PersistentReviewBookReader implements ReviewBookReader {

    private final BookRepository bookRepository;

    @Override
    public void validateBookExists(long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException();
        }
    }
}
