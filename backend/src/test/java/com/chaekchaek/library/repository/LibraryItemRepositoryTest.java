package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class LibraryItemRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("초기화된 독서 상태를 별점과 함께 저장할 수 있다")
    void should_PersistClearedReadingState_When_RatingExists() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 300, NOW);
        item.rate(new BigDecimal("4.5"), NOW.plusSeconds(1));
        item.clearReadingState();

        // when
        LibraryItem saved = libraryItemRepository.saveAndFlush(item);
        entityManager.clear();

        // then
        LibraryItem found = libraryItemRepository.findByMemberIdAndBookId(1L, 2L).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getStatus()).isNull();
        assertThat(found.getCurrentPage()).isNull();
        assertThat(found.getReadingUpdatedAt()).isNull();
        assertThat(found.getRating()).isEqualByComparingTo("4.5");
        assertThat(found.getRatingUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(found.getAddedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("부분적인 독서 상태는 저장할 수 없다")
    void should_RejectPartialReadingState_When_DatabaseCheckIsApplied() {
        // given
        LibraryItem item = libraryItemRepository.saveAndFlush(
                LibraryItem.create(1L, 2L, ReadingStatus.READING, 300, NOW)
        );

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update library_item set current_page = null where id = ?",
                item.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("별점과 갱신 시각은 함께 저장해야 한다")
    void should_RejectPartialRating_When_DatabaseCheckIsApplied() {
        // given
        LibraryItem item = LibraryItem.create(1L, 2L, ReadingStatus.READING, 300, NOW);
        item.rate(new BigDecimal("4.5"), NOW.plusSeconds(1));
        LibraryItem saved = libraryItemRepository.saveAndFlush(item);

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update library_item set rating_updated_at = null where id = ?",
                saved.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
