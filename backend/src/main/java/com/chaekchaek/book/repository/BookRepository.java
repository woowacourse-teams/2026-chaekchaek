package com.chaekchaek.book.repository;

import com.chaekchaek.book.domain.Book;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn13(String isbn13);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select book from Book book where book.id = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") long bookId);

    List<Book> findAllByIsbn13In(Collection<String> isbn13s);
}
