package com.chaekchaek.book.service;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class BookResolver {

    private final AladinBookClient bookClient;
    private final BookRepository bookRepository;
    private final TransactionTemplate newTransaction;

    public BookResolver(
            AladinBookClient bookClient,
            BookRepository bookRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.bookClient = bookClient;
        this.bookRepository = bookRepository;
        this.newTransaction = new TransactionTemplate(transactionManager);
        newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public Book findOrCreate(String isbn13) {
        validateIsbn13(isbn13);
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> registerBookFetchedOutsideTransaction(isbn13));
    }

    public Book lookup(String isbn13) {
        validateIsbn13(isbn13);
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> toBook(bookClient.findBookByIsbn13(isbn13)));
    }

    private void validateIsbn13(String isbn13) {
        if (!Isbn13.isValid(isbn13)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Book registerBookFetchedOutsideTransaction(String isbn13) {
        AladinBookItem source = bookClient.findBookByIsbn13(isbn13);
        try {
            return Objects.requireNonNull(newTransaction.execute(status -> bookRepository
                    .findByIsbn13(isbn13)
                    .orElseGet(() -> bookRepository.saveAndFlush(toBook(source)))));
        } catch (DataIntegrityViolationException exception) {
            return bookRepository.findByIsbn13(isbn13)
                    .orElseThrow(() -> exception);
        }
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
