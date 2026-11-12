package com.chaekchaek.library.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.domain.LibraryItem;
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
    @DisplayName("공유 항목이 없는 도서가 있어도 책장과 배치를 삭제한다")
    void should_DeleteLibraryAndPlacements_When_SharedItemIsMissing() {
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
        LibraryItem item = LibraryItem.create(member.getId(), recordedBook.getId(), NOW);
        item.rate(new BigDecimal("4.5"), NOW.plusSeconds(1));
        libraryItemRepository.saveAndFlush(item);
        libraryBookRepository.saveAllAndFlush(List.of(
                LibraryBook.create(library.getId(), recordedBook.getId(), 1L),
                LibraryBook.create(library.getId(), missingRecordBook.getId(), 2L)
        ));

        // when & then
        libraryBookService.deleteLibrary(member.getId(), library.getId());

        assertThat(libraryRepository.findById(library.getId())).isEmpty();
        assertThat(libraryBookRepository.findAllByLibraryId(library.getId())).isEmpty();
        assertThat(libraryItemRepository.findByMemberIdAndBookId(member.getId(), recordedBook.getId()))
                .get()
                .satisfies(saved -> {
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
                null,
                null
        );
    }
}
