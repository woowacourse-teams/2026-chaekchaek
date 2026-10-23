package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class LibraryBookRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("같은 도서를 서로 다른 책장에 배치할 수 있다")
    void should_AllowSameBook_When_LibrariesAreDifferent() {
        // given
        Library first = libraryRepository.create(Library.create(1L, "첫 번째", NOW));
        Library second = Library.create(1L, "두 번째", NOW);
        second.changeDisplayOrder(2L);
        libraryRepository.create(second);

        // when
        libraryBookRepository.saveAndFlush(LibraryBook.create(first.getId(), 100L, 1L));
        libraryBookRepository.saveAndFlush(LibraryBook.create(second.getId(), 100L, 1L));

        // then
        assertThat(libraryBookRepository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("같은 책장의 같은 도서는 한 번만 배치할 수 있다")
    void should_RejectDuplicatePlacement_When_LibraryAndBookAreSame() {
        // given
        Library library = libraryRepository.create(Library.create(1L, "가을에 읽을 책", NOW));
        libraryBookRepository.saveAndFlush(LibraryBook.create(library.getId(), 100L, 1L));
        LibraryBook duplicate = LibraryBook.create(library.getId(), 100L, 2L);

        // when & then
        assertThatThrownBy(() -> libraryBookRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("책장 배치 순서는 양수여야 한다")
    void should_RejectNonPositiveDisplayOrder_When_DatabaseCheckIsApplied() {
        // given
        Library library = libraryRepository.create(Library.create(1L, "가을에 읽을 책", NOW));
        LibraryBook placement = libraryBookRepository.saveAndFlush(
                LibraryBook.create(library.getId(), 100L, 1L)
        );

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update library_book set display_order = 0 where id = ?",
                placement.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
