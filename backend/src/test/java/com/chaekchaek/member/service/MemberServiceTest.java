package com.chaekchaek.member.service;

import static com.chaekchaek.actor.fixture.ActorFixture.savedActor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.service.AppleAccountService;
import com.chaekchaek.auth.token.refresh.RefreshToken;
import com.chaekchaek.auth.token.refresh.RefreshTokenRepository;
import com.chaekchaek.common.auth.ActorType;
import com.chaekchaek.common.exception.ErrorCode;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.AccountStatus;
import com.chaekchaek.common.exception.MemberNotFoundException;
import com.chaekchaek.common.exception.NicknameAlreadyExistsException;
import com.chaekchaek.common.exception.NicknameRequiredException;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.dto.MemberResponse;
import com.chaekchaek.member.dto.MyInfoResponse;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 13, 12, 0);

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ActorRepository actorRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private AppleAccountService appleAccountService;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 ID로 내 정보를 조회한다")
    void should_GetMyInfo_When_MemberExists() {
        // given
        Member member = Member.create(
                "덜 우아한 참새",
                "exUrl",
                LocalDateTime.of(2026, 8, 13, 12, 0)
        );
        ReflectionTestUtils.setField(member, "id", 7L);
        Actor actor = savedActor(41L, Actor.member(member, LocalDateTime.of(2026, 8, 13, 12, 0)));

        given(memberRepository.findById(7L))
                .willReturn(Optional.of(member));
        given(actorRepository.findByMemberId(7L)).willReturn(Optional.of(actor));

        // when
        MyInfoResponse response = memberService.getMyInfo(7L);

        // then
        MyInfoResponse expected = new MyInfoResponse(
                7L,
                41L,
                ActorType.MEMBER,
                null,
                "덜 우아한 참새",
                "exUrl",
                true,
                "ACTIVE"
        );
        assertThat(response).isEqualTo(expected);
    }

    @Test
    @DisplayName("관리자 내 정보에는 실제 Actor 유형을 반환한다")
    void should_GetAdminActorType_When_MemberIsAdmin() {
        // given
        Member member = Member.create("우아한 참새", null, CREATED_AT);
        Actor actor = savedActor(41L, Actor.member(member, CREATED_AT));
        actor.grantAdmin();
        given(memberRepository.findById(7L)).willReturn(Optional.of(member));
        given(actorRepository.findByMemberId(7L)).willReturn(Optional.of(actor));

        // when
        MyInfoResponse response = memberService.getMyInfo(7L);

        // then
        assertThat(response.actorType()).isEqualTo(ActorType.ADMIN);
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 예외가 발생한다")
    void should_ThrowException_When_MemberNotFound() {
        // given
        given(memberRepository.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(999L))
                .isInstanceOfSatisfying(
                MemberNotFoundException.class,
                exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND)
        );
    }

    @Test
    @DisplayName("공개 닉네임을 설정한다")
    void should_UpdateNickname_When_NicknameIsAvailable() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, CREATED_AT);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameAndIdNot("책책이", 1L)).willReturn(false);
        given(clock.instant()).willReturn(Instant.parse("2026-10-23T03:00:00Z"));
        given(libraryRepository.findMaxDisplayOrderByMemberId(1L)).willReturn(2L);

        // when
        MemberResponse response = memberService.updateNickname(1L, "책책이");

        // then
        assertAll(
                () -> assertThat(response.nickname()).isEqualTo("책책이"),
                () -> assertThat(response.displayAnonymous()).isTrue()
        );
        ArgumentCaptor<Library> captor = ArgumentCaptor.forClass(Library.class);
        verify(libraryRepository).create(captor.capture());
        assertThat(captor.getValue().isDefault()).isTrue();
        assertThat(captor.getValue().getName()).isEqualTo("책책이");
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3L);
    }

    @Test
    @DisplayName("이미 사용 중인 공개 닉네임은 설정할 수 없다")
    void should_RejectNickname_When_NicknameIsDuplicated() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, CREATED_AT);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(memberRepository.existsByNicknameAndIdNot("책책이", 1L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.updateNickname(1L, "책책이"))
                .isInstanceOf(NicknameAlreadyExistsException.class);
        verifyNoInteractions(libraryRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마바사아자차카"})
    @DisplayName("첫 닉네임이 10자를 초과하면 닉네임과 책장을 저장하지 않는다")
    void should_RejectFirstNickname_When_NicknameExceedsMaximumLength(String nickname) {
        // given
        Member member = Member.create("익명", null, CREATED_AT);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.updateNickname(1L, nickname))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
        assertThat(member.getNickname()).isNull();
        verifyNoInteractions(libraryRepository);
    }

    @Test
    @DisplayName("첫 닉네임과 같은 이름인 기존 책장을 순서와 이름 그대로 기본 책장으로 승격한다")
    void should_PromoteExistingLibrary_When_FirstNicknameMatchesItsName() {
        // given
        Member member = Member.create("익명", null, CREATED_AT);
        Library library = Library.create(1L, "Study", Instant.parse("2026-10-01T00:00:00Z"));
        library.changeDisplayOrder(4L);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(libraryRepository.findByMemberIdAndName(1L, "study")).willReturn(Optional.of(library));

        // when
        memberService.updateNickname(1L, "study");

        // then
        assertThat(library.isDefault()).isTrue();
        assertThat(library.getName()).isEqualTo("Study");
        assertThat(library.getDisplayOrder()).isEqualTo(4L);
        assertThat(library.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        verify(libraryRepository, never()).create(any());
        verifyNoInteractions(clock);
    }

    @Test
    @DisplayName("백필로 기본 책장이 있으면 첫 닉네임 저장 때 기본 책장을 추가하지 않는다")
    void should_KeepExistingDefaultLibrary_When_FirstNicknameIsSaved() {
        // given
        Member member = Member.create("익명", null, CREATED_AT);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));
        given(libraryRepository.existsByMemberIdAndIsDefaultTrue(1L)).willReturn(true);

        // when
        memberService.updateNickname(1L, "책책이");

        // then
        assertThat(member.getNickname()).isEqualTo("책책이");
        verify(libraryRepository, never()).create(any());
        verify(libraryRepository, never()).findByMemberIdAndName(anyLong(), any());
    }

    @Test
    @DisplayName("첫 저장 이후 닉네임 변경에는 기존 검증을 유지하고 책장을 바꾸지 않는다")
    void should_KeepLibraryUntouched_When_NicknameChangesAfterOnboarding() {
        // given
        Member member = Member.create("익명", null, CREATED_AT);
        member.updateNickname("원래이름");
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        // when
        memberService.updateNickname(1L, "new name");

        // then
        assertThat(member.getNickname()).isEqualTo("new name");
        verifyNoInteractions(libraryRepository, clock);
    }

    @Test
    @DisplayName("비활성 회원은 닉네임과 기본 책장을 변경할 수 없다")
    void should_RejectNicknameUpdate_When_MemberIsInactive() {
        // given
        Member member = Member.create("익명", null, CREATED_AT);
        ReflectionTestUtils.setField(member, "accountStatus", AccountStatus.SUSPENDED);
        given(memberRepository.findByIdForUpdate(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.updateNickname(1L, "책책이"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(libraryRepository);
    }

    @Test
    @DisplayName("공개 닉네임 없이 익명 상태를 해제할 수 없다")
    void should_RejectDisableAnonymity_When_NicknameIsMissing() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, CREATED_AT);
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> memberService.updateAnonymity(1L, false))
                .isInstanceOf(NicknameRequiredException.class);
    }

    @Test
    @DisplayName("공개 닉네임 설정 후 익명 상태를 해제한다")
    void should_DisableAnonymity_When_NicknameIsSet() {
        // given
        Member member = Member.create("우아한 달빛 참새", null, CREATED_AT);
        member.updateNickname("책책이");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.updateAnonymity(1L, false);

        // then
        assertThat(response.displayAnonymous()).isFalse();
    }

    @Test
    @DisplayName("회원을 탈퇴 처리한다")
    void should_WithdrawMember_When_WithdrawalIsRequested() {
        // given
        Member member = Member.create("우아한 달빛 참새", "profile", CREATED_AT);
        RefreshToken refreshToken = RefreshToken.issue(member, "token-hash", CREATED_AT, CREATED_AT.plusDays(14));
        member.updateNickname("책책이");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(1L))
                .willReturn(List.of(refreshToken));

        // when
        memberService.withdraw(1L);

        // then
        assertAll(
                () -> assertThat(member.getAccountStatus().name()).isEqualTo("WITHDRAWN"),
                () -> assertThat(member.getWithdrawnAt()).isNotNull(),
                () -> assertThat(member.getNickname()).isNull(),
                () -> assertThat(member.getProfileImageUrl()).isNull()
        );
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getRevokedAt()).isEqualTo(member.getWithdrawnAt());
        verify(appleAccountService).revokeIfConnected(1L);
    }
}
