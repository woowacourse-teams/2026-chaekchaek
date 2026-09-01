package com.chaekchaek.book.service;

import static org.springframework.web.util.HtmlUtils.htmlUnescape;

import com.chaekchaek.book.client.AladinBookClient;
import com.chaekchaek.book.client.AladinContributorParser;
import com.chaekchaek.book.client.dto.AladinBookItem;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
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

    public Book findOrCreate(Isbn13 isbn13) {
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> registerBookFetchedOutsideTransaction(isbn13));
    }

    public Book lookup(Isbn13 isbn13) {
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> toBook(isbn13, bookClient.findBookByIsbn13(isbn13)));
    }

    private Book registerBookFetchedOutsideTransaction(Isbn13 isbn13) {
        AladinBookItem source = bookClient.findBookByIsbn13(isbn13);
        try {
            return Objects.requireNonNull(newTransaction.execute(status -> bookRepository
                    .findByIsbn13(isbn13)
                    .orElseGet(() -> bookRepository.saveAndFlush(toBook(isbn13, source)))));
        } catch (DataIntegrityViolationException exception) {
            return bookRepository.findByIsbn13(isbn13)
                    .orElseThrow(() -> exception);
        }
    }

    private Book toBook(Isbn13 isbn13, AladinBookItem source) {
        AladinContributorParser.Contributors contributors = AladinContributorParser.parse(source.author());
        return Book.create(
                isbn13,
                source.title(),
                source.cover(),
                htmlUnescape(source.description()),
                contributors.authors(),
                contributors.translators(),
                source.publisher(),
                source.categoryName(),
                source.publishedDate(),
                source.totalPages()
        );
    }
}
