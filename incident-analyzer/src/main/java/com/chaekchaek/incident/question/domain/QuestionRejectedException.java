package com.chaekchaek.incident.question.domain;

public class QuestionRejectedException extends RuntimeException {
    public QuestionRejectedException(String notice) {
        super(notice);
    }
}
