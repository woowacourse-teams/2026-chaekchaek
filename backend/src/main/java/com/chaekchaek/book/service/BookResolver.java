package com.chaekchaek.book.service;

import static org.springframework.web.util.HtmlUtils.htmlUnescape;

import com.chaekchaek.book.client.BookClientException;
import com.chaekchaek.book.client.BookDetailClient;
import com.chaekchaek.book.client.BookDetailItem;
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class BookResolver {

    private static final Logger log = LoggerFactory.getLogger(BookResolver.class);

    private final BookDetailClient bookClient;
    private final BookRepository bookRepository;
    private final TransactionTemplate newTransaction;

    public BookResolver(
            BookDetailClient bookClient,
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

    private Book registerBookFetchedOutsideTransaction(Isbn13 isbn13) {
        BookDetailItem source = bookClient.findBookByIsbn13(isbn13);
        String spineImageUrl = withoutPreparingImage(source.spineImageUrl());
        try {
            return Objects.requireNonNull(newTransaction.execute(status -> bookRepository
                    .findByIsbn13(isbn13)
                    .orElseGet(() -> bookRepository.saveAndFlush(toBook(isbn13, source, spineImageUrl)))));
        } catch (DataIntegrityViolationException exception) {
            return bookRepository.findByIsbn13(isbn13)
                    .orElseThrow(() -> exception);
        }
    }

    private String withoutPreparingImage(String spineImageUrl) {
        if (spineImageUrl == null) {
            return null;
        }
        try {
            if (bookClient.isPreparingImage(spineImageUrl)) {
                return null;
            }
        } catch (BookClientException exception) {
            log.atWarn()
                    .addKeyValue("type", "error")
                    .addKeyValue("imageUrl", spineImageUrl)
                    .setCause(exception)
                    .log("책등 이미지 확인 실패");
        }
        return spineImageUrl;
    }

    public Book lookup(Isbn13 isbn13) {
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> {
                    BookDetailItem source = bookClient.findBookByIsbn13(isbn13);
                    return toBook(isbn13, source, source.spineImageUrl());
                });
    }

    private Book toBook(Isbn13 isbn13, BookDetailItem source, String spineImageUrl) {
        return Book.create(
                isbn13,
                source.title(),
                source.coverImageUrl(),
                spineImageUrl,
                source.backImageUrl(),
                unescapeDescription(source.description()),
                source.authors(),
                source.translators(),
                source.publisher(),
                source.category(),
                source.publishedDate(),
                source.purchaseUrl(),
                source.source()
        );
    }

    private String unescapeDescription(String description) {
        if (description == null) {
            return null;
        }
        return htmlUnescape(description);
    }
}
