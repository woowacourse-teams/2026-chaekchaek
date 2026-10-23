package com.chaekchaek.book.client.yes24;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "yes24")
public record Yes24Properties(
        @NotBlank String baseUrl,
        @NotBlank String apiKey
) {

}
