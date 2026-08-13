package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.exception.BookNotFoundException;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final AladinBookClient bookClient;
    private final BookRepository bookRepository;

    @Transactional
    public synchronized BookDetailResponse resolve(String isbn13) {
        Book book = bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> createFromAladin(isbn13));
        return toDetailResponse(book);
    }

    private Book createFromAladin(String isbn13) {
        AladinBookItem source = bookClient.findBookByIsbn13(isbn13);
        return bookRepository.save(toBook(source));
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

    private Book toBook(AladinBookItem source) {
        AladinContributorParser.Contributors contributors = AladinContributorParser.parse(source.author());
        LocalDate publishedDate = source.pubDate() == null || source.pubDate().isBlank()
                ? null
                : LocalDate.parse(source.pubDate());
        return Book.create(
                source.isbn13(),
                source.title(),
                source.cover(),
                contributors.authors(),
                contributors.translators(),
                source.publisher(),
                source.categoryName(),
                publishedDate,
                source.totalPages()
        );
    }
}
