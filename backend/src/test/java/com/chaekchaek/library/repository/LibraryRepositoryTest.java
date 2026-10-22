package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.library.domain.Library;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class LibraryRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("서재를 저장하면 소유 멤버와 정규화된 이름을 다시 조회할 수 있다")
    void should_PersistLibrary_When_Created() {
        // given
        Library library = Library.create(1L, "  가을에 읽을 책  ", NOW);

        // when
        Library saved = libraryRepository.create(library);
        entityManager.clear();

        // then
        Library found = libraryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isOwnedBy(1L)).isTrue();
        assertThat(found.getName()).isEqualTo("가을에 읽을 책");
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("같은 멤버가 서로 다른 이름의 서재를 만들 수 있다")
    void should_AllowMultipleLibraries_When_MemberUsesDifferentNames() {
        // given
        libraryRepository.create(Library.create(1L, "가을에 읽을 책", NOW));

        // when
        libraryRepository.create(Library.create(1L, "겨울에 읽을 책", NOW));

        // then
        assertThat(libraryRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 멤버는 같은 이름의 서재를 만들 수 있다")
    void should_AllowSameName_When_MembersAreDifferent() {
        // given
        libraryRepository.create(Library.create(1L, "가을에 읽을 책", NOW));

        // when
        libraryRepository.create(Library.create(2L, "가을에 읽을 책", NOW));

        // then
        assertThat(libraryRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 멤버의 정규화된 서재 이름이 중복되면 업무 오류를 반환한다")
    void should_RejectDuplicateName_When_MemberIsSame() {
        // given
        libraryRepository.create(Library.create(1L, "가을에 읽을 책", NOW));
        Library duplicate = Library.create(1L, "  가을에 읽을 책  ", NOW);

        // when & then
        assertThatThrownBy(() -> libraryRepository.create(duplicate))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS)
                );
    }
}
