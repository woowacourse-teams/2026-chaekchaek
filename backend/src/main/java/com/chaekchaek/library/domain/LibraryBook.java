package com.chaekchaek.library.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "library_book",
        check = @CheckConstraint(
                name = "ck_library_book_display_order_positive",
                constraint = "display_order > 0"
        ),
        indexes = {
                @Index(
                        name = "ix_library_book_library_order_id",
                        columnList = "library_id, display_order, id"
                ),
                @Index(
                        name = "ix_library_book_book_library",
                        columnList = "book_id, library_id"
                )
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_library_book_library_book",
                columnNames = {"library_id", "book_id"}
        )
)
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "library_id", nullable = false)
    private long libraryId;

    @Column(name = "book_id", nullable = false)
    private long bookId;

    @Column(name = "display_order", nullable = false)
    private long displayOrder;

    protected LibraryBook() {
    }

    private LibraryBook(long libraryId, long bookId, long displayOrder) {
        this.libraryId = libraryId;
        this.bookId = bookId;
        this.displayOrder = displayOrder;
    }

    public static LibraryBook create(long libraryId, long bookId, long displayOrder) {
        if (libraryId < 1 || bookId < 1 || displayOrder < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new LibraryBook(libraryId, bookId, displayOrder);
    }

    public void changeDisplayOrder(long displayOrder) {
        if (displayOrder < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public long getLibraryId() {
        return libraryId;
    }

    public long getBookId() {
        return bookId;
    }

    public long getDisplayOrder() {
        return displayOrder;
    }
}
