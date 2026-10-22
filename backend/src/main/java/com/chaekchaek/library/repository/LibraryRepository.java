package com.chaekchaek.library.repository;

import com.chaekchaek.library.domain.Library;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibraryRepository extends JpaRepository<Library, Long> {

    default Library create(Library library) {
        try {
            return saveAndFlush(library);
        } catch (DataIntegrityViolationException exception) {
            throw LibraryCreationExceptionTranslator.translate(exception);
        }
    }
}
