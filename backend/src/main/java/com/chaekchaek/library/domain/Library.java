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
import java.time.Instant;

@Entity
@Table(
        name = "library",
        check = @CheckConstraint(
                name = "ck_library_display_order_positive",
                constraint = "display_order > 0"
        ),
        indexes = @Index(
                name = "ix_library_member_order_id",
                columnList = "member_id, display_order, id"
        ),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_libraries_member_name",
                columnNames = {"member_id", "name"}
        )
)
public class Library {

    private static final int NAME_COLUMN_LENGTH = 104;
    private static final int MAX_NAME_LENGTH = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(nullable = false, length = NAME_COLUMN_LENGTH)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "display_order", nullable = false)
    private long displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Library() {
    }

    private Library(long memberId, String name, Instant createdAt) {
        this(memberId, name, 1, createdAt);
    }

    private Library(long memberId, String name, long displayOrder, Instant createdAt) {
        this.memberId = memberId;
        this.name = name;
        this.isDefault = false;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    public static Library create(long memberId, String name, Instant now) {
        validateNewName(name);
        return new Library(memberId, name, now);
    }

    public static Library createDefault(
            long memberId,
            String name,
            long displayOrder,
            Instant now
    ) {
        validateNewName(name);
        Library library = new Library(memberId, name, now);
        library.changeDisplayOrder(displayOrder);
        library.promoteToDefault();
        return library;
    }

    public static void validateNewName(String name) {
        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    public void promoteToDefault() {
        this.isDefault = true;
    }

    public boolean rename(String name, Instant now) {
        validateNewName(name);
        if (this.name.equals(name)) {
            return false;
        }
        this.name = name;
        this.updatedAt = now;
        return true;
    }

    public void changeDisplayOrder(long displayOrder) {
        if (displayOrder < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.displayOrder = displayOrder;
    }

    public void assertDeletable() {
        if (isDefault) {
            throw new BusinessException(ErrorCode.DEFAULT_LIBRARY_DELETE_NOT_ALLOWED);
        }
    }

    public boolean isOwnedBy(long memberId) {
        return this.memberId == memberId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public long getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
