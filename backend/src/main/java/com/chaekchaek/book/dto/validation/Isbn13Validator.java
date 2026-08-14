package com.chaekchaek.book.dto.validation;

import com.chaekchaek.book.domain.Isbn13;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Isbn13Validator implements ConstraintValidator<ValidIsbn13, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && Isbn13.isValid(value);
    }
}
