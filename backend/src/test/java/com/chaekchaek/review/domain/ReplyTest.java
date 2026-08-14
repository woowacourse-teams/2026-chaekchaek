package com.chaekchaek.review.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chaekchaek.common.exception.BusinessException;
import com.chaekchaek.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReplyTest {

    @Test
    @DisplayName("작성자만 답글을 수정할 수 있다")
    void should_UpdateReply_When_AuthorUpdates() {
        // given
        Reply reply = Reply.create(1L, 2L, "답글", false);

        // when
        reply.updateBy(2L, "수정 답글");

        // then
        assertThat(reply.getContent()).isEqualTo("수정 답글");
    }

    @Test
    @DisplayName("다른 회원은 답글을 삭제할 수 없다")
    void should_ThrowForbidden_When_NonAuthorDeletesReply() {
        // given
        Reply reply = Reply.create(1L, 2L, "답글", false);

        // when & then
        assertThatThrownBy(() -> reply.deleteBy(3L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
