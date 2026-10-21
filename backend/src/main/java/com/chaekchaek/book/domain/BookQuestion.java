package com.chaekchaek.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "book_questions",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_questions_isbn13", columnNames = "isbn13")
)
public class BookQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 13)
    @Convert(converter = Isbn13Converter.class)
    private Isbn13 isbn13;

    @Column(nullable = false, length = 1_000)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String plot;

    @Column(columnDefinition = "TEXT")
    private String extract;

    protected BookQuestion() {
    }

    private BookQuestion(Isbn13 isbn13, String question, String plot, String extract) {
        this.isbn13 = isbn13;
        this.question = question;
        this.plot = plot;
        this.extract = extract;
    }

    public static BookQuestion create(Isbn13 isbn13, String question, String plot, String extract) {
        return new BookQuestion(isbn13, question, plot, extract);
    }

    public Long getId() {
        return id;
    }

    public Isbn13 getIsbn13() {
        return isbn13;
    }

    public String getQuestion() {
        return question;
    }

    public String getPlot() {
        return plot;
    }

    public String getExtract() {
        return extract;
    }
}
