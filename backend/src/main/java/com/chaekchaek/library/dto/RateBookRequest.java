package com.chaekchaek.library.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RateBookRequest(
        @NotNull @DecimalMin("0.1") @DecimalMax("5.0") @Digits(integer = 1, fraction = 1)
        BigDecimal rating
) {
}
