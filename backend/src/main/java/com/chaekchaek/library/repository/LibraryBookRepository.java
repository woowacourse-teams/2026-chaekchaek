package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.LibraryBook;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    boolean existsByLibraryIdAndBookId(long libraryId, long bookId);

    Optional<LibraryBook> findByLibraryIdAndBookId(long libraryId, long bookId);

    long countByLibraryId(long libraryId);

    @Query("""
            select coalesce(max(libraryBook.displayOrder), 0)
            from LibraryBook libraryBook
            where libraryBook.libraryId = :libraryId
            """)
    long findMaxDisplayOrderByLibraryId(@Param("libraryId") long libraryId);

    @Query("""
            select count(libraryBook)
            from LibraryBook libraryBook
            where libraryBook.bookId = :bookId
              and libraryBook.libraryId in (
                  select library.id
                  from Library library
                  where library.memberId = :memberId
              )
            """)
    long countByMemberIdAndBookId(@Param("memberId") long memberId, @Param("bookId") long bookId);

    default LibraryBook create(LibraryBook libraryBook) {
        try {
            return saveAndFlush(libraryBook);
        } catch (DataIntegrityViolationException exception) {
            throw LibraryBookCreationExceptionTranslator.translate(exception);
        }
    }
}
