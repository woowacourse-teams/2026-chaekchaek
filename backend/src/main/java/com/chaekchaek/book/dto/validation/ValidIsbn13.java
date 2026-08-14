package com.chaekchaek.book.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Isbn13Validator.class)
public @interface ValidIsbn13 {

    String message() default "ISBN13은 체크섬이 유효한 숫자 13자리여야 합니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
