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

    private final BookResolver bookResolver;
    private final BookRepository bookRepository;

    public BookDetailResponse resolve(String isbn13) {
        return toDetailResponse(bookResolver.resolve(isbn13));
    }

    @Transactional(readOnly = true)
    public BookDetailResponse getDetail(long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(BookNotFoundException::new);
        return toDetailResponse(book);
    }

    private BookDetailResponse toDetailResponse(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getIsbn13(),
                book.getTitle(),
                book.getCoverImageUrl(),
                book.getAuthors(),
                book.getTranslators(),
                book.getPublisher(),
                book.getCategory(),
                book.getPublishedDate() == null ? null : book.getPublishedDate().toString(),
                book.getTotalPages(),
                0,
                null,
                0,
                null
        );
    }
}
