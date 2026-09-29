package com.chaekchaek.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.review.domain.Review;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ReviewFeedRepositoryTest {

    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("스포일러를 포함하고 삭제된 감상과 책이 없는 감상을 제외해 페이지를 조회한다")
    void should_PageVisibleReviews_When_ReviewsIncludeSpoilersDeletedAndMissingBooks() {
        // given
        long bookId = bookRepository.save(book()).getId();
        List<Review> visible = new ArrayList<>();
        for (int index = 0; index < 21; index++) {
            visible.add(reviewRepository.save(review(bookId, "감상 " + index, index == 20)));
        }
        Review deleted = reviewRepository.save(review(bookId, "삭제 감상", false));
        deleted.deleteBy(1L);
        reviewRepository.save(review(999_999L, "책 없는 감상", false));
        reviewRepository.flush();

        // when
        Page<Review> first = reviewRepository.findFeedReviews(PageRequest.of(0, 20, LATEST_FIRST));
        Page<Review> second = reviewRepository.findFeedReviews(PageRequest.of(1, 20, LATEST_FIRST));
        Page<Review> beyond = reviewRepository.findFeedReviews(PageRequest.of(2, 20, LATEST_FIRST));

        // then
        assertThat(first.getTotalElements()).isEqualTo(21);
        assertThat(first.getContent()).hasSize(20);
        assertThat(first.hasNext()).isTrue();
        List<Long> expectedIds = visible.reversed().subList(0, 20)
                .stream()
                .map(Review::getId)
                .toList();
        assertThat(first.getContent()).extracting(Review::getId)
                .containsExactlyElementsOf(expectedIds);
        assertThat(first.getContent().getFirst().isSpoiler()).isTrue();
        assertThat(second.getContent()).extracting(Review::getId).containsExactly(visible.getFirst().getId());
        assertThat(second.hasNext()).isFalse();
        assertThat(beyond.getContent()).isEmpty();
        assertThat(beyond.getTotalElements()).isEqualTo(21);
    }

    @Test
    @DisplayName("감상이 정확히 스무 개면 다음 페이지가 없다")
    void should_HaveNoNextPage_When_ExactlyTwentyReviewsExist() {
        // given
        long bookId = bookRepository.save(book()).getId();
        for (int index = 0; index < 20; index++) {
            reviewRepository.save(review(bookId, "감상 " + index, false));
        }
        reviewRepository.flush();

        // when
        Page<Review> result = reviewRepository.findFeedReviews(PageRequest.of(0, 20, LATEST_FIRST));

        // then
        assertThat(result.getTotalElements()).isEqualTo(20);
        assertThat(result.getContent()).hasSize(20);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("작성 시각이 같으면 감상 ID가 큰 순서로 조회한다")
    void should_OrderByReviewIdDescending_When_CreatedAtIsEqual() {
        // given
        long bookId = bookRepository.save(book()).getId();
        Review first = reviewRepository.save(review(bookId, "첫 감상", false));
        Review second = reviewRepository.save(review(bookId, "둘째 감상", false));
        reviewRepository.flush();
        Timestamp sameTime = Timestamp.from(Instant.parse("2026-09-28T10:00:00Z"));
        jdbcTemplate.update("update review set created_at = ? where review_id in (?, ?)",
                sameTime, first.getId(), second.getId());
        entityManager.clear();

        // when
        Page<Review> result = reviewRepository.findFeedReviews(PageRequest.of(0, 20, LATEST_FIRST));

        // then
        assertThat(result.getContent()).extracting(Review::getId)
                .containsExactly(second.getId(), first.getId());
    }

    private Book book() {
        return Book.create(
                new Isbn13("9788936433598"),
                "도서",
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("저자"),
                List.of(),
                "출판사",
                "소설",
                null,
                null
        );
    }

    private Review review(long bookId, String content, boolean spoiler) {
        return Review.create(
                bookId,
                1L,
                content,
                null,
                null,
                null,
                spoiler,
                false
        );
    }
}
