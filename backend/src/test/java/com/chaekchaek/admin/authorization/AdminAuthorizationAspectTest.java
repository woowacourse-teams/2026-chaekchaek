package com.chaekchaek.admin.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chaekchaek.common.auth.CurrentActor;
import com.chaekchaek.common.auth.CurrentActorProvider;
import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AdminAuthorizationAspectTest {

    private final CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);
    private final AdminAuthorizationAspect aspect = new AdminAuthorizationAspect(currentActorProvider);

    @Test
    @DisplayName("관리자는 권한 검사를 통과한다")
    void should_AllowInvocation_When_ActorIsAdmin() {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(CurrentActor.admin(1L, 1L));

        // when & then
        assertThatCode(aspect::checkAdmin).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("nonAdminActors")
    @DisplayName("관리자가 아닌 사용자의 실행을 거절한다")
    void should_RejectInvocation_When_ActorIsNotAdmin(CurrentActor actor) {
        // given
        when(currentActorProvider.getCurrentActor()).thenReturn(actor);

        // when & then
        assertThatThrownBy(aspect::checkAdmin)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN)
                );
    }

    private static Stream<CurrentActor> nonAdminActors() {
        return Stream.of(CurrentActor.member(2L, 2L), CurrentActor.guest(3L));
    }

    @Test
    @DisplayName("인증 정보가 없으면 인증 오류를 전달한다")
    void should_RejectInvocation_When_ActorIsNotAuthenticated() {
        // given
        when(currentActorProvider.getCurrentActor()).thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(aspect::checkAdmin)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED)
                );
    }
}
