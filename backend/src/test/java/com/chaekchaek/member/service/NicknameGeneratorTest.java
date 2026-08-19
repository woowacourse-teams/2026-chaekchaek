package com.chaekchaek.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NicknameGeneratorTest {

    private final NicknameGenerator nicknameGenerator = new NicknameGenerator();

    @Test
    @DisplayName("형용사와 색상으로 참새 닉네임을 생성한다")
    void should_GenerateNickname_WithAdjectiveAndColor() {
        IntStream.range(0, 100)
                .mapToObj(ignored -> nicknameGenerator.generate())
                .forEach(nickname -> assertThat(nickname)
                        .isNotBlank()
                        .endsWith(" 참새")
                        .containsPattern("^.+ .+ 참새$")
                        .hasSizeLessThanOrEqualTo(100));
    }
}
