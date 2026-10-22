package com.chaekchaek.library.domain;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
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
        name = "libraries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_libraries_member_name",
                columnNames = {"member_id", "name"}
        )
)
public class Library {

    private static final int NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Library() {
    }

    private Library(long memberId, String name, Instant createdAt) {
        this.memberId = memberId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public static Library create(long memberId, String name, Instant now) {
        validateName(name);
        return new Library(memberId, name, now);
    }

    private static void validateName(String name) {
        if (name == null || name.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
