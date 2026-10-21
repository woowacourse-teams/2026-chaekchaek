package com.chaekchaek.book.repository;

import com.chaekchaek.book.domain.BookQuestion;
import com.chaekchaek.book.domain.Isbn13;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookQuestionRepository extends JpaRepository<BookQuestion, Long> {

    Optional<BookQuestion> findByIsbn13(Isbn13 isbn13);
}
