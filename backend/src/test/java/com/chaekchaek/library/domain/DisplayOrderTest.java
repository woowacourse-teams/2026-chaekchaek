package com.chaekchaek.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DisplayOrderTest {

    @Test
    @DisplayName("순서가 달라도 전체 ID 집합이 같으면 허용한다")
    void should_AcceptDifferentOrder_When_AllIdsArePresent() {
        // given
        DisplayOrder order = new DisplayOrder(List.of(3L, 1L, 2L));

        // when & then
        assertThatCode(() -> order.requireSameIds(List.of(1L, 2L, 3L))).doesNotThrowAnyException();
        assertThat(order.ids()).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("현재 전체 목록이 비어 있으면 빈 배열을 허용한다")
    void should_AcceptEmptyOrder_When_CurrentIdsAreEmpty() {
        // given
        DisplayOrder order = new DisplayOrder(List.of());

        // when & then
        assertThatCode(() -> order.requireSameIds(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이전된 30권 초과 목록도 전체 ID를 검증한다")
    void should_AcceptMoreThanThirtyIds_When_AllIdsArePresent() {
        // given
        List<Long> ids = LongStream.rangeClosed(1, 35)
                .boxed()
                .toList();
        DisplayOrder order = new DisplayOrder(ids.reversed());

        // when & then
        assertThatCode(() -> order.requireSameIds(ids)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    @DisplayName("누락된 배열과 null, 비양수, 중복 ID는 잘못된 요청이다")
    void should_RejectInvalidIds_When_CreatingOrder(List<Long> ids) {
        // when & then
        assertThatThrownBy(() -> new DisplayOrder(ids))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    static List<List<Long>> invalidIds() {
        return Arrays.asList(null, Arrays.asList(1L, null), List.of(0L), List.of(-1L), List.of(1L, 1L));
    }

    @ParameterizedTest
    @MethodSource("differentIds")
    @DisplayName("누락, 초과, 같은 개수의 다른 ID와 빈 배열은 현재 집합이 다르면 충돌이다")
    void should_RejectConflict_When_CurrentIdsAreDifferent(List<Long> ids) {
        // given
        DisplayOrder order = new DisplayOrder(ids);

        // when & then
        assertThatThrownBy(() -> order.requireSameIds(List.of(1L, 2L)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_CONFLICT));
    }

    static List<List<Long>> differentIds() {
        return List.of(List.of(), List.of(1L), List.of(1L, 3L), List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("입력과 반환 목록의 외부 변경으로 순서가 바뀌지 않는다")
    void should_PreserveOrder_When_SourceListChanges() {
        // given
        List<Long> ids = new ArrayList<>(List.of(Long.MAX_VALUE, 1L));
        DisplayOrder order = new DisplayOrder(ids);

        // when
        ids.clear();

        // then
        assertThat(order.ids()).containsExactly(Long.MAX_VALUE, 1L);
        assertThatThrownBy(() -> order.ids().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("배치 순서 변경은 양의 BIGINT를 허용하고 비양수를 거절한다")
    void should_ValidatePositiveOrder_When_ChangingPlacementOrder() {
        // given
        LibraryBook placement = LibraryBook.create(1L, 10L, 1L);

        // when
        placement.changeDisplayOrder(Long.MAX_VALUE);

        // then
        assertThat(placement.getDisplayOrder()).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(() -> placement.changeDisplayOrder(0L)).isInstanceOf(BusinessException.class);
        assertThat(placement.getDisplayOrder()).isEqualTo(Long.MAX_VALUE);
    }
}
