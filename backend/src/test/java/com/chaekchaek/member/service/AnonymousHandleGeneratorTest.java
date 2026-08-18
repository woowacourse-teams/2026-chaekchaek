package com.chaekchaek.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnonymousHandleGeneratorTest {

    private static final String ANONYMOUS_HANDLE_PATTERN =
            "참새-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final AnonymousHandleGenerator anonymousHandleGenerator =
            new AnonymousHandleGenerator();

    @Test
    @DisplayName("전체 UUID를 사용한 익명 핸들을 생성한다")
    void should_GenerateFullUuidHandle_When_Called() {
        // when
        String anonymousHandle = anonymousHandleGenerator.generate();

        // then
        assertThat(anonymousHandle).matches(ANONYMOUS_HANDLE_PATTERN);
    }
}
