package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.LibraryBook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    boolean existsByLibraryIdAndBookId(long libraryId, long bookId);

    long countByLibraryId(long libraryId);

    @Query("""
            select coalesce(max(libraryBook.displayOrder), 0)
            from LibraryBook libraryBook
            where libraryBook.libraryId = :libraryId
            """)
    long findMaxDisplayOrderByLibraryId(@Param("libraryId") long libraryId);

    default LibraryBook create(LibraryBook libraryBook) {
        try {
            return saveAndFlush(libraryBook);
        } catch (DataIntegrityViolationException exception) {
            throw LibraryBookCreationExceptionTranslator.translate(exception);
        }
    }
}
