package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class DefaultLibraryTest {

    private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마바사아자차", "Study_1-2", "소낙 눈", "닉네임!", "A"})
    @DisplayName("첫 닉네임을 그대로 사용해 기본 책장을 지정한 순서에 만든다")
    void should_CreateDefaultLibrary_When_NicknameIsValid(String nickname) {
        // when
        Library library = Library.createDefault(7L, nickname, 4L, NOW);

        // then
        assertThat(library.getName()).isEqualTo(nickname);
        assertThat(library.isDefault()).isTrue();
        assertThat(library.isOwnedBy(7L)).isTrue();
        assertThat(library.getDisplayOrder()).isEqualTo(4L);
        assertThat(library.getCreatedAt()).isEqualTo(NOW);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003", "가나다라마바사아자차카"})
    @DisplayName("기본 책장의 이름이 공백뿐이거나 10자를 초과하면 거부한다")
    void should_RejectDefaultLibrary_When_NicknameIsInvalid(String nickname) {
        // when & then
        assertThatThrownBy(() -> Library.createDefault(7L, nickname, 1L, NOW))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST)
                );
    }

    @Test
    @DisplayName("기존 책장 승격은 저장된 이름과 순서와 생성 시각을 보존한다")
    void should_PreserveLegacyLibrary_When_PromotingToDefault() {
        // given
        Library library = Library.create(7L, "Legacy", NOW);
        ReflectionTestUtils.setField(library, "name", "예전 책장 이름! ".repeat(5));
        library.changeDisplayOrder(8L);
        String oldName = library.getName();

        // when
        library.promoteToDefault();

        // then
        assertThat(library.isDefault()).isTrue();
        assertThat(library.getName()).isEqualTo(oldName);
        assertThat(library.getDisplayOrder()).isEqualTo(8L);
        assertThat(library.getCreatedAt()).isEqualTo(NOW);
        assertThat(library.getUpdatedAt()).isNull();
    }
}
