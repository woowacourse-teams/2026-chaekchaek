package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class LibraryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    @DisplayName("책장을 생성하면 이름과 생성 시각을 갖고 수정 시각은 비어 있다")
    void should_HaveNameAndCreatedAtWithoutUpdatedAt_When_Created() {
        // given
        long memberId = 1L;
        String name = "가을책장";

        // when
        Library library = Library.create(memberId, name, NOW);

        // then
        assertThat(library.getName()).isEqualTo(name);
        assertThat(library.isDefault()).isFalse();
        assertThat(library.getDisplayOrder()).isEqualTo(1L);
        assertThat(library.getCreatedAt()).isEqualTo(NOW);
        assertThat(library.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("책장 표시 순서는 양수여야 한다")
    void should_RejectNonPositiveDisplayOrder_When_ChangingDisplayOrder() {
        Library library = Library.create(1L, "가을책장", NOW);

        assertThatThrownBy(() -> library.changeDisplayOrder(0L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @Test
    @DisplayName("허용된 문자 10자로 책장을 생성할 수 있다")
    void should_CreateLibrary_When_NameHasMaximumLength() {
        String name = "가".repeat(10);

        Library library = Library.create(1L, name, NOW);

        assertThat(library.getName()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"가을 책", " 가을책", "가을책 ", "가을\t책", "책장!", "book.name"})
    @DisplayName("공백이나 허용되지 않는 문자가 포함되면 생성을 거부한다")
    void should_RejectCreation_When_NameContainsUnsupportedCharacters(String name) {
        assertInvalidName(name);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    @DisplayName("책장 이름이 비어 있으면 생성을 거부한다")
    void should_RejectCreation_When_NameIsBlank(String name) {
        assertInvalidName(name);
    }

    @Test
    @DisplayName("책장 이름이 없으면 생성을 거부한다")
    void should_RejectCreation_When_NameIsNull() {
        assertInvalidName(null);
    }

    @Test
    @DisplayName("책장 이름이 10자를 초과하면 생성을 거부한다")
    void should_RejectCreation_When_NameExceedsMaximumLength() {
        assertInvalidName("가".repeat(11));
    }

    @ParameterizedTest
    @CsvSource({"1000, true", "2000, false"})
    @DisplayName("책장이 지정한 멤버의 소유인지 판단한다")
    void should_IdentifyOwnership_When_MemberIsGiven(long memberId, boolean expected) {
        Library library = Library.create(1000L, "가을책장", NOW);

        boolean owned = library.isOwnedBy(memberId);

        assertThat(owned).isEqualTo(expected);
    }

    @Test
    @DisplayName("일반 책장은 삭제할 수 있다")
    void should_AllowDeletion_When_LibraryIsNotDefault() {
        Library library = Library.create(1L, "가을책장", NOW);

        library.assertDeletable();
    }

    @Test
    @DisplayName("기본 책장 삭제를 거부한다")
    void should_RejectDeletion_When_LibraryIsDefault() {
        Library library = Library.create(1L, "책책", NOW);
        ReflectionTestUtils.setField(library, "isDefault", true);

        assertThatThrownBy(library::assertDeletable)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.DEFAULT_LIBRARY_DELETE_NOT_ALLOWED)
                );
    }

    @Test
    @DisplayName("책장 이름을 변경해도 기본 여부와 표시 순서를 유지하고 수정 시각을 갱신한다")
    void should_RenameLibraryAndPreserveOtherProperties_When_NewNameIsValid() {
        Library library = Library.createDefault(1L, "책책", 5L, NOW);

        boolean changed = library.rename("읽을책", NOW.plusSeconds(1));

        assertThat(changed).isTrue();
        assertThat(library.getName()).isEqualTo("읽을책");
        assertThat(library.isDefault()).isTrue();
        assertThat(library.getDisplayOrder()).isEqualTo(5L);
        assertThat(library.getCreatedAt()).isEqualTo(NOW);
        assertThat(library.getUpdatedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    @DisplayName("현재 이름과 같은 이름은 변경하지 않고 수정 시각을 유지한다")
    void should_NotChangeLibrary_When_NameIsUnchanged() {
        Library library = Library.create(1L, "가을책장", NOW);

        boolean changed = library.rename("가을책장", NOW.plusSeconds(1));

        assertThat(changed).isFalse();
        assertThat(library.getName()).isEqualTo("가을책장");
        assertThat(library.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("책장 이름 변경에 허용되지 않는 이름이면 기존 책장을 변경하지 않는다")
    void should_NotChangeLibrary_When_NewNameIsInvalid() {
        Library library = Library.create(1L, "가을책장", NOW);

        assertThatThrownBy(() -> library.rename("가을 책", NOW.plusSeconds(1)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
        assertThat(library.getName()).isEqualTo("가을책장");
        assertThat(library.getUpdatedAt()).isNull();
    }

    private void assertInvalidName(String name) {
        assertThatThrownBy(() -> Library.create(1L, name, NOW))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }
}
