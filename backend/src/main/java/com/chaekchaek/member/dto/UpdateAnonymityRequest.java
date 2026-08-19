package com.chaekchaek.member.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAnonymityRequest(
        @NotNull Boolean displayAnonymous
) {
}
