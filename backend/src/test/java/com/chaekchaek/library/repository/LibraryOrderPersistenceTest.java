package com.chaekchaek.library.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.domain.LibraryBook;
import com.chaekchaek.library.service.LibraryOrderService;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(LibraryOrderService.class)
@ActiveProfiles("test")
class LibraryOrderPersistenceTest {

    @Autowired
    private LibraryOrderService service;
    @Autowired
    private MemberRepository members;
    @Autowired
    private LibraryRepository libraries;
    @Autowired
    private LibraryBookRepository placements;
    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("같은 순서값이 있던 책장도 요청 순서로 영속화하고 다른 회원은 유지한다")
    void should_PersistLibraryOrder_When_ExistingPositionsOverlap() {
        // given
        Member member = member();
        Library first = library(member.getId(), "첫째", 9L);
        Library second = library(member.getId(), "둘째", 9L);
        Library other = library(member.getId() + 100, "다른회원", 7L);

        // when
        service.reorderLibraries(member.getId(), List.of(second.getId(), first.getId()));
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(libraries.findAllByMemberIdOrderByDisplayOrderAscIdAsc(member.getId()))
                .extracting(Library::getId).containsExactly(second.getId(), first.getId());
        assertThat(libraries.findById(other.getId()).orElseThrow().getDisplayOrder()).isEqualTo(7L);
    }

    @Test
    @DisplayName("35권의 도서 순서를 저장하고 다른 책장의 같은 도서 순서는 유지한다")
    void should_PersistThirtyFiveOrders_When_MigratedLibraryIsReordered() {
        // given
        Member member = member();
        Library target = library(member.getId(), "대상", 1L);
        Library other = library(member.getId(), "다른책장", 2L);
        List<Long> ids = LongStream.rangeClosed(1L, 35L)
                .boxed()
                .toList();
        ids.forEach(id -> placements.save(LibraryBook.create(target.getId(), id, 99L)));
        placements.saveAndFlush(LibraryBook.create(other.getId(), 1L, 55L));

        // when
        service.reorderBooks(member.getId(), target.getId(), ids.reversed());
        entityManager.flush();
        entityManager.clear();

        // then
        List<LibraryBook> ordered = placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(target.getId());
        assertThat(ordered).extracting(LibraryBook::getBookId).containsExactlyElementsOf(ids.reversed());
        assertThat(ordered).extracting(LibraryBook::getDisplayOrder).containsExactlyElementsOf(ids);
        assertThat(placements.findByLibraryIdAndBookId(other.getId(), 1L).orElseThrow().getDisplayOrder())
                .isEqualTo(55L);
    }

    @Test
    @DisplayName("집합 충돌 시 도서 순서를 저장하지 않는다")
    void should_KeepStoredOrder_When_BookIdsConflict() {
        // given
        Member member = member();
        Library library = library(member.getId(), "대상", 1L);
        placements.saveAndFlush(LibraryBook.create(library.getId(), 10L, 5L));
        placements.saveAndFlush(LibraryBook.create(library.getId(), 20L, 9L));

        // when & then
        assertThatThrownBy(() -> service.reorderBooks(member.getId(), library.getId(), List.of(20L, 30L)))
                .isInstanceOf(BusinessException.class);
        entityManager.flush();
        entityManager.clear();
        assertThat(placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(library.getId()))
                .extracting(LibraryBook::getDisplayOrder).containsExactly(5L, 9L);
    }

    @Test
    @DisplayName("빈 서재와 빈 책장의 전체 순서 변경을 저장할 수 있다")
    void should_AcceptEmptyIds_When_NoRowsExist() {
        // given
        Member member = member();

        // when
        service.reorderLibraries(member.getId(), List.of());
        Library library = library(member.getId(), "빈책장", 1L);
        service.reorderBooks(member.getId(), library.getId(), List.of());
        entityManager.flush();

        // then
        assertThat(placements.findAllByLibraryIdOrderByDisplayOrderAscIdAsc(library.getId())).isEmpty();
    }

    private Member member() {
        return members.saveAndFlush(Member.create("순서익명", null, LocalDateTime.of(2026, 10, 23, 0, 0)));
    }

    private Library library(long memberId, String name, long position) {
        Library library = Library.create(memberId, name, Instant.parse("2026-10-23T00:00:00Z"));
        library.changeDisplayOrder(position);
        return libraries.saveAndFlush(library);
    }
}
