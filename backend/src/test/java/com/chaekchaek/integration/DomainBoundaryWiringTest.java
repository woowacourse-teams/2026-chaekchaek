package com.chaekchaek.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.library.service.BookCommentCountReader;
import com.chaekchaek.review.book.ReviewBookReader;
import com.chaekchaek.review.library.ReadingRecordCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DomainBoundaryWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("도서·서재·감상 경계에는 실제 구현만 연결된다")
    void should_WireConcreteImplementations_When_ApplicationStarts() {
        // when
        var commentCountReaders = applicationContext.getBeansOfType(BookCommentCountReader.class);
        var reviewBookReaders = applicationContext.getBeansOfType(ReviewBookReader.class);
        var readingRecordCoordinators = applicationContext.getBeansOfType(ReadingRecordCoordinator.class);

        // then
        assertThat(commentCountReaders).containsOnlyKeys("reviewService");
        assertThat(reviewBookReaders).containsOnlyKeys("persistentReviewBookReader");
        assertThat(readingRecordCoordinators).containsOnlyKeys("libraryReadingRecordCoordinator");
    }
}
