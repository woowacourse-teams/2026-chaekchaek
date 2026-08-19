package com.chaekchaek.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank @Size(max = 100) String nickname
) {
}
