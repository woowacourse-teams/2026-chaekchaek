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
    @DisplayName("책장을 저장하면 소유 멤버와 입력한 이름을 다시 조회할 수 있다")
    void should_PersistLibrary_When_Created() {
        // given
        Library library = Library.create(1L, "가을책장", NOW);

        // when
        Library saved = libraryRepository.create(library);
        entityManager.clear();

        // then
        Library found = libraryRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isOwnedBy(1L)).isTrue();
        assertThat(found.getName()).isEqualTo("가을책장");
        assertThat(found.isDefault()).isFalse();
        assertThat(found.getDisplayOrder()).isEqualTo(1L);
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("같은 멤버가 서로 다른 이름의 서재를 만들 수 있다")
    void should_AllowMultipleLibraries_When_MemberUsesDifferentNames() {
        // given
        libraryRepository.create(Library.create(1L, "가을책장", NOW));

        // when
        libraryRepository.create(Library.create(1L, "겨울책장", NOW));

        // then
        assertThat(libraryRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 멤버는 같은 이름의 서재를 만들 수 있다")
    void should_AllowSameName_When_MembersAreDifferent() {
        // given
        libraryRepository.create(Library.create(1L, "가을책장", NOW));

        // when
        libraryRepository.create(Library.create(2L, "가을책장", NOW));

        // then
        assertThat(libraryRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("멤버별 책장 순서 중 가장 큰 값을 조회한다")
    void should_FindMaxDisplayOrder_When_MemberHasLibraries() {
        // given
        Library first = Library.create(1L, "첫번째", NOW);
        first.changeDisplayOrder(2L);
        libraryRepository.create(first);
        Library second = Library.create(1L, "두번째", NOW);
        second.changeDisplayOrder(5L);
        libraryRepository.create(second);
        Library anotherMember = Library.create(2L, "다른회원", NOW);
        anotherMember.changeDisplayOrder(9L);
        libraryRepository.create(anotherMember);

        // when
        long actual = libraryRepository.findMaxDisplayOrderByMemberId(1L);

        // then
        assertThat(actual).isEqualTo(5L);
    }

    @Test
    @DisplayName("같은 멤버의 책장 이름이 중복되면 업무 오류를 반환한다")
    void should_RejectDuplicateName_When_MemberIsSame() {
        // given
        libraryRepository.create(Library.create(1L, "가을책장", NOW));
        Library duplicate = Library.create(1L, "가을책장", NOW);

        // when & then
        assertThatThrownBy(() -> libraryRepository.create(duplicate))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS)
                );
    }

    @Test
    @DisplayName("같은 멤버의 다른 책장 이름과 중복되면 이름 변경 오류를 반환한다")
    void should_RejectDuplicateName_When_RenamedLibraryHasSameMemberName() {
        // given
        libraryRepository.create(Library.create(1L, "가을책장", NOW));
        Library library = libraryRepository.create(Library.create(1L, "읽을책", NOW));
        library.rename("가을책장", NOW.plusSeconds(1));

        // when & then
        assertThatThrownBy(() -> libraryRepository.rename(library))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.LIBRARY_NAME_ALREADY_EXISTS)
                );
    }

    @Test
    @DisplayName("다른 멤버의 이름과 겹쳐도 책장 이름을 변경할 수 있다")
    void should_AllowSameName_When_RenamedLibrariesBelongToDifferentMembers() {
        // given
        libraryRepository.create(Library.create(2L, "가을책장", NOW));
        Library library = libraryRepository.create(Library.create(1L, "읽을책", NOW));
        library.rename("가을책장", NOW.plusSeconds(1));

        // when
        libraryRepository.rename(library);

        // then
        entityManager.clear();
        assertThat(libraryRepository.findById(library.getId()).orElseThrow().getName())
                .isEqualTo("가을책장");
        assertThat(libraryRepository.count()).isEqualTo(2);
    }
}
