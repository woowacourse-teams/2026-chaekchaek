package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.LibraryBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {
}
