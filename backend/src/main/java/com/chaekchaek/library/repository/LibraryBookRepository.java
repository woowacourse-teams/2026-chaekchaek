package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.LibraryBook;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBookRepository extends JpaRepository<LibraryBook, Long> {

    List<LibraryBook> findAllByLibraryIdOrderByDisplayOrderAscIdAsc(long libraryId);

    boolean existsByLibraryIdAndBookId(long libraryId, long bookId);

    Optional<LibraryBook> findByLibraryIdAndBookId(long libraryId, long bookId);

    @Query("""
            select distinct p.bookId
            from LibraryBook p
            where p.bookId in :bookIds
              and p.libraryId in (
                  select l.id
                  from Library l
                  where l.memberId = :memberId
              )
            """)
    List<Long> findPlacedBookIdsByMemberIdAndBookIdIn(
            @Param("memberId") long memberId,
            @Param("bookIds") Collection<Long> bookIds
    );

    long countByLibraryId(long libraryId);

    List<LibraryBook> findAllByLibraryId(long libraryId);

    List<LibraryBook> findAllByLibraryIdAndBookIdIn(long libraryId, Collection<Long> bookIds);

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
