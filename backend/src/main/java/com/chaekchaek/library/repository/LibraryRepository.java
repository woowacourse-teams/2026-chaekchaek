package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.Library;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library, Long> {

    @Query("""
            select coalesce(max(l.displayOrder), 0)
            from Library l
            where l.memberId = :memberId
            """)
    long findMaxDisplayOrderByMemberId(@Param("memberId") long memberId);

    default Library create(Library library) {
        try {
            return saveAndFlush(library);
        } catch (DataIntegrityViolationException exception) {
            throw LibraryCreationExceptionTranslator.translate(exception);
        }
    }
}
