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

class LibraryTest {

    private static final Instant NOW = Instant.parse("2026-10-22T00:00:00Z");

    @Test
    @DisplayName("서재를 생성하면 이름과 생성 시각을 갖고 수정 시각은 비어 있다")
    void should_HaveNameAndCreatedAtWithoutUpdatedAt_When_Created() {
        // given
        long memberId = 1L;
        String name = "가을에 읽을 책";

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
    @DisplayName("서재 표시 순서는 양수여야 한다")
    void should_RejectNonPositiveDisplayOrder_When_ChangingDisplayOrder() {
        // when & then
        Library library = Library.create(1L, "가을에 읽을 책", NOW);
        assertThatThrownBy(() -> library.changeDisplayOrder(0L))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @Test
    @DisplayName("서재 이름이 10자이면 생성할 수 있다")
    void should_CreateLibrary_When_NameHasMaximumLength() {
        // given
        String name = "가".repeat(10);

        // when
        Library library = Library.create(1L, name, NOW);

        // then
        assertThat(library.getName()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  가을에  읽을 책  ",
            "\t가을에  읽을 책\n",
            "\u2003가을에  읽을 책\u2003"
    })
    @DisplayName("서재 이름의 앞뒤 공백을 제거하고 내부 공백은 유지한다")
    void should_StripSurroundingWhitespace_When_CreatingLibrary(String name) {
        // given
        String expectedName = "가을에  읽을 책";

        // when
        Library library = Library.create(1L, name, NOW);

        // then
        assertThat(library.getName()).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("앞뒤 공백을 제거한 서재 이름이 10자이면 생성할 수 있다")
    void should_CreateLibrary_When_StrippedNameHasMaximumLength() {
        // given
        String expectedName = "가".repeat(10);
        String name = "  " + expectedName + "  ";

        // when
        Library library = Library.create(1L, name, NOW);

        // then
        assertThat(library.getName()).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    @DisplayName("앞뒤 공백을 제거한 서재 이름이 비어 있으면 생성을 거부한다")
    void should_RejectCreation_When_StrippedNameIsEmpty(String name) {
        // given
        long memberId = 1L;

        // when & then
        assertThatThrownBy(() -> Library.create(memberId, name, NOW))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @Test
    @DisplayName("서재 이름이 없으면 생성을 거부한다")
    void should_RejectCreation_When_NameIsNull() {
        // given
        String name = null;

        // when & then
        assertThatThrownBy(() -> Library.create(1L, name, NOW))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @Test
    @DisplayName("서재 이름이 10자를 초과하면 생성을 거부한다")
    void should_RejectCreation_When_NameExceedsMaximumLength() {
        // given
        String name = "가".repeat(11);

        // when & then
        assertThatThrownBy(() -> Library.create(1L, name, NOW))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @ParameterizedTest
    @CsvSource({"1000, true", "2000, false"})
    @DisplayName("서재가 지정한 멤버의 소유인지 판단한다")
    void should_IdentifyOwnership_When_MemberIsGiven(long memberId, boolean expected) {
        // given
        Library library = Library.create(1000L, "가을에 읽을 책", NOW);

        // when
        boolean owned = library.isOwnedBy(memberId);

        // then
        assertThat(owned).isEqualTo(expected);
    }
}
