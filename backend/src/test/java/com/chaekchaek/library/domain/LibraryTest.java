package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        assertThat(library.getCreatedAt()).isEqualTo(NOW);
        assertThat(library.getUpdatedAt()).isNull();
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
