package com.chaekchaek.book.dto;

public record BookQuestionResponse(
        String question,
        String plot,
        String extract
) {
}
