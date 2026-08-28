package com.chaekchaek.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "recommended_books",
        uniqueConstraints = @UniqueConstraint(name = "uk_recommended_books_book_id", columnNames = "book_id")
)
public class RecommendedBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private long bookId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecommendedBook() {
    }

    private RecommendedBook(long bookId, Instant createdAt) {
        this.bookId = bookId;
        this.createdAt = createdAt;
    }

    public static RecommendedBook create(long bookId, Instant createdAt) {
        return new RecommendedBook(bookId, createdAt);
    }

    public Long getId() {
        return id;
    }

    public long getBookId() {
        return bookId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
