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
import com.chaekchaek.book.domain.Book;
import com.chaekchaek.book.domain.Isbn13;
import com.chaekchaek.book.repository.BookRepository;
import java.time.LocalDate;
import java.util.List;
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
    @Autowired BookRepository bookRepository;

    @Test
    void should_CreateReview_When_GuestTokenUsesIsbnReviewPath() throws Exception {
        // given
        String guestToken = issueGuestToken();
        Book book = bookRepository.save(Book.create(
                new Isbn13("9788925568683"), "마션", "https://example.com/martian.jpg", "책 설명",
                List.of("앤디 위어"), List.of(), "알에이치코리아", "SF", LocalDate.of(2026, 1, 1), 308
        ));

        // when & then
        mockMvc.perform(post("/api/v1/books/by-isbn/{isbn13}/reviews", book.getIsbn13().value())
                        .header(SecurityContextCurrentActorProvider.GUEST_TOKEN_HEADER, guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"게스트 감상\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.review.content").value("게스트 감상"));
    }

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

        mockMvc.perform(post("/api/v1/books/by-isbn/{isbn13}/reviews", "9788925568683")
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
