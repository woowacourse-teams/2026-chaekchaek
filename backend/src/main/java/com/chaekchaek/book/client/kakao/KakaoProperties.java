package com.chaekchaek.book.client.kakao;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        @NotBlank String baseUrl,
        @NotBlank String restApiKey
) {
}
