package com.chaekchaek.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.chaekchaek.auth.principal.SecurityContextCurrentActorProvider;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestInteractionAuthorizationIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void guestTokenPassesPublicInteractionSecurityButNotMemberOnlySecurity() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(post("/api/v1/books/999/reviews")
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"게스트 감상\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/library?page=1")
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestCannotMutateLibraryStatusPageOrRating() throws Exception {
        String guestToken = issueGuestToken();

        mockMvc.perform(post("/api/v1/library")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn13\":\"9788936433598\",\"status\":\"WANT_TO_READ\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/library/1")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPage\":10,\"totalPages\":100}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/library/1")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READING\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/library/1/rating")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4.5}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/library/1/rating")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/library/1")
                        .with(csrf())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicInteractionStillRequiresAnActor() throws Exception {
        mockMvc.perform(post("/api/v1/books/999/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"식별자 없는 감상\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String issueGuestToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/guest-token"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("guestToken").asText();
    }
}
