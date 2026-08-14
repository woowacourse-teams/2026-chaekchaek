package com.chaekchaek.book.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "books",
        uniqueConstraints = @UniqueConstraint(name = "uk_books_isbn13", columnNames = "isbn13")
)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 13)
    private String isbn13;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1_000)
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "book_authors", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "name", nullable = false)
    @OrderColumn(name = "display_order")
    private List<String> authors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "book_translators", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "name", nullable = false)
    @OrderColumn(name = "display_order")
    private List<String> translators = new ArrayList<>();

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private String category;

    private LocalDate publishedDate;

    private Integer totalPages;

    protected Book() {
    }

    private Book(
            String isbn13,
            String title,
            String coverImageUrl,
            List<String> authors,
            List<String> translators,
            String publisher,
            String category,
            LocalDate publishedDate,
            Integer totalPages
    ) {
        this.isbn13 = isbn13;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.authors = new ArrayList<>(authors);
        this.translators = new ArrayList<>(translators);
        this.publisher = publisher;
        this.category = category;
        this.publishedDate = publishedDate;
        this.totalPages = totalPages;
    }

    public static Book create(
            String isbn13,
            String title,
            String coverImageUrl,
            List<String> authors,
            List<String> translators,
            String publisher,
            String category,
            LocalDate publishedDate,
            Integer totalPages
    ) {
        if (!Isbn13.isValid(isbn13)) {
            throw new IllegalArgumentException("ISBN13 must have a valid checksum");
        }
        validateTotalPages(totalPages);
        return new Book(isbn13, title, coverImageUrl, authors, translators, publisher, category,
                publishedDate, totalPages);
    }

    public void rememberTotalPages(Integer requestedTotalPages) {
        if (requestedTotalPages == null) {
            return;
        }
        validateTotalPages(requestedTotalPages);
        if (totalPages == null) {
            totalPages = requestedTotalPages;
            return;
        }
        if (!totalPages.equals(requestedTotalPages)) {
            throw new BusinessException(ErrorCode.TOTAL_PAGES_CONFLICT);
        }
    }

    private static void validateTotalPages(Integer totalPages) {
        if (totalPages != null && totalPages <= 0) {
            throw new IllegalArgumentException("Total pages must be positive");
        }
    }

    public Long getId() {
        return id;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getTitle() {
        return title;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public List<String> getAuthors() {
        return List.copyOf(authors);
    }

    public List<String> getTranslators() {
        return List.copyOf(translators);
    }

    public String getPublisher() {
        return publisher;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public Integer getTotalPages() {
        return totalPages;
    }
}
