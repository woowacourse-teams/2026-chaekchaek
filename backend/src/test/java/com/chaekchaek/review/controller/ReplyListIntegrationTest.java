package com.chaekchaek.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.auth.controller.GuestAuthController.GuestTokenResponse;
import com.chaekchaek.auth.principal.SecurityContextCurrentActorProvider;
import com.chaekchaek.review.domain.Review;
import com.chaekchaek.review.repository.ReviewRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReplyListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("삭제한 답글은 다시 조회한 답글 목록, 전체 개수, 다음 페이지에서 제외한다")
    void should_ExcludeDeletedReply_When_FindingRepliesAfterDeletion() throws Exception {
        // given
        GuestTokenResponse guest = issueGuestToken();
        String guestToken = guest.guestToken();
        Review review = reviewRepository.save(Review.create(1L, guest.actorId(), "감상", null, null, null, false, false));
        List<Long> replyIds = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            replyIds.add(createReply(review.getId(), guestToken));
        }
        mockMvc.perform(delete("/api/v1/replies/{replyId}", replyIds.getFirst())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isNoContent());

        // when
        String response = mockMvc.perform(get("/api/v1/reviews/{reviewId}/replies", review.getId())
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        JsonNode actual = objectMapper.readTree(response);
        List<Long> actualReplyIds = actual.get("items")
                .valueStream()
                .map(item -> item.get("replyId").asLong())
                .toList();
        assertThat(actual.get("totalCount").asLong()).isEqualTo(10);
        assertThat(actual.get("nextPage").isNull()).isTrue();
        assertThat(actualReplyIds).containsExactlyElementsOf(replyIds.subList(1, 11));
    }

    @ParameterizedTest
    @CsvSource({
            "PATCH, /api/v1/replies/{replyId}",
            "DELETE, /api/v1/replies/{replyId}",
            "POST, /api/v1/replies/{replyId}/reactions",
            "DELETE, /api/v1/replies/{replyId}/reactions"
    })
    @DisplayName("삭제된 답글을 수정, 삭제하거나 좋아요를 등록, 취소하면 찾을 수 없다고 응답한다")
    void should_ReturnReplyNotFound_When_MutatingDeletedReply(String method, String path) throws Exception {
        // given
        GuestTokenResponse guest = issueGuestToken();
        String guestToken = guest.guestToken();
        Review review = reviewRepository.save(Review.create(1L, guest.actorId(), "감상", null, null, null, false, false));
        long replyId = createReply(review.getId(), guestToken);
        mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", replyId)
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/replies/{replyId}", replyId)
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isNoContent());
        MockHttpServletRequestBuilder mutationRequest = request(HttpMethod.valueOf(method), path, replyId)
                .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken);
        if (HttpMethod.PATCH.matches(method)) {
            mutationRequest.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정 답글\"}");
        }

        // when & then
        mockMvc.perform(mutationRequest)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPLY_NOT_FOUND"));
    }

    @Test
    @DisplayName("부모 감상이 삭제되면 답글 작성과 기존 답글의 좋아요 등록은 충돌로 응답한다")
    void should_ReturnDeletedResource_When_CreatingReplyOrReactionUnderDeletedReview() throws Exception {
        // given
        GuestTokenResponse guest = issueGuestToken();
        String guestToken = guest.guestToken();
        Review review = reviewRepository.save(Review.create(1L, guest.actorId(), "감상", null, null, null, false, false));
        long replyId = createReply(review.getId(), guestToken);
        review.deleteBy(guest.actorId());
        reviewRepository.saveAndFlush(review);

        // when & then
        mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", review.getId())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"답글\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DELETED_RESOURCE"));
        mockMvc.perform(post("/api/v1/replies/{replyId}/reactions", replyId)
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DELETED_RESOURCE"));
    }

    private GuestTokenResponse issueGuestToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/guest-token"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, GuestTokenResponse.class);
    }

    private long createReply(long reviewId, String guestToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/reviews/{reviewId}/replies", reviewId)
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"답글\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("replyId").asLong();
    }
}
