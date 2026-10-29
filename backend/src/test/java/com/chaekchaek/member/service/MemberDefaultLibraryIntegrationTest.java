package com.chaekchaek.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class MemberDefaultLibraryIntegrationTest {

    @Autowired
    private MemberService memberService;

    @MockitoSpyBean
    private MemberRepository memberRepository;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @DisplayName("첫 닉네임과 같은 기존 책장을 새로 만들지 않고 기본으로 승격한다")
    void should_PromoteExistingLibrary_When_SavingFirstNickname() {
        // given
        Member member = memberRepository.saveAndFlush(Member.create("익명", null, LocalDateTime.now()));
        Library existing = Library.create(member.getId(), "Study", Instant.parse("2026-10-01T00:00:00Z"));
        existing.changeDisplayOrder(4L);
        libraryRepository.create(existing);
        Long libraryId = existing.getId();

        // when
        memberService.updateNickname(member.getId(), "Study");
        entityManager.flush();
        entityManager.clear();

        // then
        Library saved = libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(libraryId);
        assertThat(saved.getName()).isEqualTo("Study");
        assertThat(saved.getDisplayOrder()).isEqualTo(4L);
        assertThat(saved.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isEqualTo("Study");
    }

    @Test
    @Transactional
    @DisplayName("백필 기본 책장의 이름은 첫 닉네임과 후속 닉네임 변경에도 유지한다")
    void should_PreserveBackfilledDefaultLibrary_When_NicknameIsSavedAndChanged() {
        // given
        Member member = memberRepository.saveAndFlush(Member.create("익명", null, LocalDateTime.now()));
        Library existing = Library.create(member.getId(), "Legacy", Instant.now());
        ReflectionTestUtils.setField(existing, "name", "기본 책장");
        existing.promoteToDefault();
        libraryRepository.create(existing);

        // when
        memberService.updateNickname(member.getId(), "First");
        memberService.updateNickname(member.getId(), "Second");
        entityManager.flush();
        entityManager.clear();

        // then
        Library saved = libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId()).orElseThrow();
        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getName()).isEqualTo("기본 책장");
        assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isEqualTo("Second");
    }

    @Test
    @DisplayName("닉네임 유일 제약이 저장 시 실패하면 먼저 생성한 기본 책장도 롤백한다")
    void should_RollBackDefaultLibrary_When_NicknameWriteConflictsAfterPrecheck() {
        // given
        Member existing = Member.create("기존익명", null, LocalDateTime.now());
        existing.updateNickname("Taken");
        memberRepository.saveAndFlush(existing);
        Member member = memberRepository.saveAndFlush(Member.create("신규익명", null, LocalDateTime.now()));
        doReturn(false).when(memberRepository).existsByNicknameAndIdNot("Taken", member.getId());

        try {
            // when & then
            assertThatThrownBy(() -> memberService.updateNickname(member.getId(), "Taken"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(memberRepository.findById(member.getId()).orElseThrow().getNickname()).isNull();
            assertThat(libraryRepository.findByMemberIdAndIsDefaultTrue(member.getId())).isEmpty();
        } finally {
            List<Library> createdLibraries = libraryRepository.findAll()
                    .stream()
                    .filter(library -> library.isOwnedBy(member.getId()))
                    .toList();
            libraryRepository.deleteAll(createdLibraries);
            memberRepository.deleteAllById(List.of(existing.getId(), member.getId()));
        }
    }

    @Test
    @DisplayName("같은 첫 닉네임을 동시에 저장해도 기본 책장을 한 개만 생성한다")
    void should_CreateOneDefaultLibrary_When_FirstNicknameRequestsAreConcurrent() throws Exception {
        // given
        Member member = memberRepository.saveAndFlush(Member.create("동시익명", null, LocalDateTime.now()));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<MemberResponse> updateNickname = () -> {
            ready.countDown();
            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Nickname requests did not start");
            }
            return memberService.updateNickname(member.getId(), "Parallel");
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(updateNickname);
            var second = executor.submit(updateNickname);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS).nickname()).isEqualTo("Parallel");
            assertThat(second.get(15, TimeUnit.SECONDS).nickname()).isEqualTo("Parallel");

            // then
            assertThat(libraryRepository.findAll())
                    .filteredOn(library -> library.isOwnedBy(member.getId()))
                    .singleElement()
                    .satisfies(library -> {
                        assertThat(library.isDefault()).isTrue();
                        assertThat(library.getName()).isEqualTo("Parallel");
                    });
        } finally {
            start.countDown();
            libraryRepository.deleteAll(libraryRepository.findAll()
                    .stream()
                    .filter(library -> library.isOwnedBy(member.getId()))
                    .toList());
            memberRepository.deleteById(member.getId());
        }
    }
}
