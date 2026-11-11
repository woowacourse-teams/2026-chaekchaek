package com.chaekchaek.book.domain;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookTest {

    @Test
    @DisplayName("책은 검증된 ISBN13 값 객체를 보관한다")
    void should_ReturnIsbn13ValueObject_When_BookIsCreated() {
        // given
        Book book = book();

        // when & then
        assertThat(book.getIsbn13()).isEqualTo(new Isbn13("9788925568683"));
    }

    @Test
    @DisplayName("책등과 책뒷면 이미지 URL을 보관한다")
    void should_KeepSpineAndBackImageUrls_When_BookIsCreated() {
        // given
        Book book = book();

        // when & then
        assertThat(book.getSpineImageUrl())
                .isEqualTo("https://image.example/martian.jpg/side");
        assertThat(book.getBackImageUrl())
                .isEqualTo("https://image.example/martian.jpg/back");
    }

    private Book book() {
        return Book.create(
                new Isbn13("9788925568683"),
                "마션",
                "https://image.example/martian.jpg",
                "https://image.example/martian.jpg/side",
                "https://image.example/martian.jpg/back",
                null,
                List.of("앤디 위어"),
                List.of("박아람"),
                "알에이치코리아",
                "SF",
                LocalDate.of(2026, 1, 1),
                "https://www.yes24.com/product/goods/101375809",
                "yes24"
        );
    }
}
