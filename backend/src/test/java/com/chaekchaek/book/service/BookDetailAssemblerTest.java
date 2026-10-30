package com.chaekchaek.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.dto.BookDetailResponse;
import com.chaekchaek.book.dto.BookMyRecordResponse;
import com.chaekchaek.book.dto.BookQuestionResponse;
import com.chaekchaek.common.auth.CurrentMemberIdProvider;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.service.BookActivityCountReader;
import com.chaekchaek.library.service.BookActivityCountReader.ActivityCounts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BookDetailAssemblerTest {

    private static final long BOOK_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final int TOTAL_PAGES = 308;
    private static final Instant NOW = Instant.parse("2026-10-27T00:00:00Z");

    private final BookActivityCountReader activityCountReader = mock(BookActivityCountReader.class);
    private final CurrentMemberIdProvider currentMemberIdProvider = mock(CurrentMemberIdProvider.class);
    private final LibraryItemRepository libraryItemRepository = mock(LibraryItemRepository.class);
    private final LibraryBookRepository libraryBookRepository = mock(LibraryBookRepository.class);
    private final BookDetailAssembler assembler = new BookDetailAssembler(
            activityCountReader,
            currentMemberIdProvider,
            libraryItemRepository,
            libraryBookRepository
    );

    @Test
    @DisplayName("책 정보와 활동 집계, 내 기록을 상세 응답으로 조합한다")
    void should_CombineBookStatisticsAndMyRecord_When_AssemblingDetail() {
        // given
        Book book = savedMartian();
        book.updateQuestionContent("화성에 홀로 남는다면 무엇부터 하시겠어요?", "줄거리", "발췌문");
        LibraryItem libraryItem = readingRecord(120);
        libraryItem.rate(new BigDecimal("4.2"), NOW);
        LibraryItemRepository.RatingStatistics ratingStatistics = mock(
                LibraryItemRepository.RatingStatistics.class
        );
        when(activityCountReader.getActivityCounts(List.of(BOOK_ID)))
                .thenReturn(Map.of(
                        BOOK_ID, new ActivityCounts(2L, 7L)
                ));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(BOOK_ID)))
                .thenReturn(List.of(ratingStatistics));
        when(ratingStatistics.getAverageRating()).thenReturn(4.24);
        when(ratingStatistics.getRatingCount()).thenReturn(3L);
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(MEMBER_ID));
        when(libraryItemRepository.countByMemberIdAndRatingIsNotNull(MEMBER_ID)).thenReturn(12L);
        when(libraryItemRepository.findByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(Optional.of(libraryItem));
        when(libraryBookRepository.countByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(2L);
        BookDetailResponse expected = new BookDetailResponse(
                1L,
                "9788925568683",
                "마션",
                "https://image.example/martian.jpg",
                "책 설명",
                List.of("앤디 위어"),
                List.of("박아람"),
                "알에이치코리아",
                "SF",
                "2026-01-01",
                308,
                2,
                7,
                new BigDecimal("4.2"),
                3,
                12,
                new BookMyRecordResponse(
                        "READING",
                        120,
                        new BigDecimal("4.2")
                ),
                true,
                "https://www.yes24.com/product/goods/101375809",
                "yes24",
                new BookQuestionResponse(
                        "화성에 홀로 남는다면 무엇부터 하시겠어요?",
                        "줄거리",
                        "발췌문"
                )
        );

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response)
                .usingRecursiveComparison()
                .withComparatorForFields(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, "averageRating")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("로그인했지만 서재 기록이 없으면 책 상세의 내 기록은 null이다")
    void should_ReturnNullMyRecord_When_AuthenticatedMemberHasNoLibraryItem() {
        // given
        Book book = savedMartian();
        when(activityCountReader.getActivityCounts(List.of(BOOK_ID)))
                .thenReturn(Map.of(BOOK_ID, ActivityCounts.ZERO));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(BOOK_ID))).thenReturn(List.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(MEMBER_ID));
        when(libraryItemRepository.findByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(Optional.empty());

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.myRecord()).isNull();
        assertThat(response.isRegisteredInMyLibrary()).isFalse();
        assertThat(response.purchaseUrl()).isEqualTo("https://www.yes24.com/product/goods/101375809");
        assertThat(response.source()).isEqualTo("yes24");
    }

    @Test
    @DisplayName("독서 기록만 초기화된 항목은 도서 상세의 내 기록에서 제외한다")
    void should_ReturnNullMyRecord_When_ReadingStateAndRatingAreMissing() {
        // given
        Book book = savedMartian();
        LibraryItem item = readingRecord(120);
        item.clearReadingState();
        when(activityCountReader.getActivityCounts(List.of(BOOK_ID))).thenReturn(Map.of(BOOK_ID, ActivityCounts.ZERO));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(BOOK_ID))).thenReturn(List.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(MEMBER_ID));
        when(libraryItemRepository.countByMemberIdAndRatingIsNotNull(MEMBER_ID)).thenReturn(0L);
        when(libraryItemRepository.findByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(Optional.of(item));

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.myRecord()).isNull();
        assertThat(response.isRegisteredInMyLibrary()).isFalse();
    }

    @Test
    @DisplayName("마지막 배치 삭제 뒤 별점만 남으면 독서 필드는 null로 반환한다")
    void should_ReturnRatingWithoutReadingState_When_ReadingStateIsCleared() {
        // given
        Book book = savedMartian();
        LibraryItem item = readingRecord(120);
        item.rate(new BigDecimal("4.5"), NOW);
        item.clearReadingState();
        when(activityCountReader.getActivityCounts(List.of(BOOK_ID))).thenReturn(Map.of(BOOK_ID, ActivityCounts.ZERO));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(BOOK_ID))).thenReturn(List.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(MEMBER_ID));
        when(libraryItemRepository.countByMemberIdAndRatingIsNotNull(MEMBER_ID)).thenReturn(1L);
        when(libraryItemRepository.findByMemberIdAndBookId(MEMBER_ID, BOOK_ID)).thenReturn(Optional.of(item));

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.myRecord()).extracting("status", "currentPage", "myRating")
                .containsExactly(null, null, new BigDecimal("4.5"));
        assertThat(response.isRegisteredInMyLibrary()).isFalse();
    }

    @Test
    @DisplayName("등록된 질문이 없는 책이면 상세 응답의 질문은 null이다")
    void should_ReturnNullBookQuestion_When_BookHasNoQuestion() {
        // given
        Book book = martian(null, "책 설명");

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.bookQuestion()).isNull();
    }

    @Test
    @DisplayName("등록되지 않은 책이어도 ISBN13으로 등록된 질문을 상세 응답에 포함한다")
    void should_IncludeBookQuestion_When_BookIsNotRegistered() {
        // given
        Book book = martian(null, "책 설명");
        book.updateQuestionContent("화성에 홀로 남는다면 무엇부터 하시겠어요?", null, null);

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.bookId()).isNull();
        assertThat(response.bookQuestion()).extracting("question", "plot", "extract")
                .containsExactly("화성에 홀로 남는다면 무엇부터 하시겠어요?", null, null);
    }

    @Test
    @DisplayName("비로그인 사용자의 상세 응답에는 내 별점 등록 수를 포함하지 않는다")
    void should_ReturnNullMyRatingCount_When_Unauthenticated() {
        // given
        Book book = savedMartian();
        when(activityCountReader.getActivityCounts(List.of(BOOK_ID)))
                .thenReturn(Map.of(BOOK_ID, ActivityCounts.ZERO));
        when(libraryItemRepository.findRatingStatisticsByBookIdIn(List.of(BOOK_ID))).thenReturn(List.of());
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.empty());

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.myRatingCount()).isNull();
        assertThat(response.isRegisteredInMyLibrary()).isNull();
        verify(libraryItemRepository, never()).countByMemberIdAndRatingIsNotNull(anyLong());
    }

    @Test
    @DisplayName("등록되지 않은 책이면 메타데이터만 상세 응답으로 조합한다")
    void should_ReturnMetadataOnly_When_BookIsNotRegistered() {
        // given
        Book book = martian(null, "책 설명");

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.bookId()).isNull();
        assertThat(response.reviewCount()).isNull();
        assertThat(response.replyCount()).isNull();
        assertThat(response.averageRating()).isNull();
        assertThat(response.ratingCount()).isNull();
        assertThat(response.myRatingCount()).isNull();
        assertThat(response.isRegisteredInMyLibrary()).isNull();
        assertThat(response.myRecord()).isNull();
        verifyNoInteractions(activityCountReader, libraryItemRepository, libraryBookRepository);
    }

    @Test
    @DisplayName("설명이 없는 기존 도서면 상세 응답의 설명을 null로 반환한다")
    void should_ReturnNullDescription_When_StoredBookHasNoDescription() {
        // given
        Book book = martian(BOOK_ID, null);

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.description()).isNull();
    }

    @Test
    @DisplayName("저장되지 않은 도서는 로그인 회원에게 책장 미등록으로 반환한다")
    void should_ReturnFalseMembership_When_AuthenticatedBookHasNoId() {
        // given
        Book book = martian(null, "책 설명");
        when(currentMemberIdProvider.findCurrentMemberId()).thenReturn(OptionalLong.of(MEMBER_ID));

        // when
        var response = assembler.assemble(book);

        // then
        assertThat(response.isRegisteredInMyLibrary()).isFalse();
        verifyNoInteractions(libraryBookRepository);
    }

    private Book savedMartian() {
        return martian(BOOK_ID, "책 설명");
    }

    private Book martian(Long id, String description) {
        Book book = Book.create(
                new Isbn13("9788925568683"),
                "마션",
                "https://image.example/martian.jpg",
                null,
                null,
                description,
                List.of("앤디 위어"),
                List.of("박아람"),
                "알에이치코리아",
                "SF",
                LocalDate.of(2026, 1, 1),
                TOTAL_PAGES,
                "https://www.yes24.com/product/goods/101375809",
                null
        );
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private LibraryItem readingRecord(int currentPage) {
        LibraryItem item = LibraryItem.create(
                MEMBER_ID,
                BOOK_ID,
                ReadingStatus.READING,
                TOTAL_PAGES,
                NOW
        );
        item.changeCurrentPage(currentPage, TOTAL_PAGES, NOW);
        return item;
    }
}
