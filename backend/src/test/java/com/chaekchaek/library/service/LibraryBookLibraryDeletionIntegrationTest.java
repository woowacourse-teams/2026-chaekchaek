package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
import com.chaekchaek.library.domain.ReadingStatus;
import com.chaekchaek.library.repository.LibraryBookRepository;
import com.chaekchaek.library.repository.LibraryItemRepository;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LibraryBookLibraryDeletionIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T03:30:00Z");

    @Autowired
    private LibraryBookService libraryBookService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryBookRepository libraryBookRepository;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("도서 공유 기록이 누락되면 책장 삭제와 배치 삭제를 함께 롤백한다")
    void should_RollBackLibraryAndPlacements_When_SharedItemIsMissing() {
        // given
        Member member = memberRepository.saveAndFlush(Member.create(
                "삭제 검증 " + UUID.randomUUID(),
                null,
                LocalDateTime.now()
        ));
        Library library = libraryRepository.create(Library.create(
                member.getId(),
                "검" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                NOW
        ));
        Book recordedBook = bookRepository.saveAndFlush(book(9788936433598L));
        Book missingRecordBook = bookRepository.saveAndFlush(book(9788936433599L));
        LibraryItem item = LibraryItem.create(member.getId(), recordedBook.getId(), ReadingStatus.READING, 368, NOW);
        item.rate(new BigDecimal("4.5"), NOW.plusSeconds(1));
        libraryItemRepository.saveAndFlush(item);
        libraryBookRepository.saveAllAndFlush(List.of(
                LibraryBook.create(library.getId(), recordedBook.getId(), 1L),
                LibraryBook.create(library.getId(), missingRecordBook.getId(), 2L)
        ));

        // when & then
        assertThatThrownBy(() -> libraryBookService.deleteLibrary(member.getId(), library.getId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(libraryRepository.findById(library.getId())).isPresent();
        assertThat(libraryBookRepository.findAllByLibraryId(library.getId()))
                .extracting(LibraryBook::getBookId)
                .containsExactlyInAnyOrder(recordedBook.getId(), missingRecordBook.getId());
        assertThat(libraryItemRepository.findByMemberIdAndBookId(member.getId(), recordedBook.getId()))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getStatus()).isEqualTo(ReadingStatus.READING);
                    assertThat(saved.getRating()).isEqualByComparingTo("4.5");
                });
    }

    private Book book(long isbn) {
        return Book.create(
                new Isbn13(Long.toString(isbn)),
                "삭제 검증 도서",
                "https://example.com/cover.jpg",
                null,
                null,
                null,
                List.of("작가"),
                List.of(),
                "출판사",
                "분류",
                null,
                368,
                null,
                null
        );
    }
}
