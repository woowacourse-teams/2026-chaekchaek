package com.chaekchaek.book.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class Isbn13Converter implements AttributeConverter<Isbn13, String> {

    @Override
    public String convertToDatabaseColumn(Isbn13 attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Isbn13 convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new Isbn13(dbData);
    }
}
