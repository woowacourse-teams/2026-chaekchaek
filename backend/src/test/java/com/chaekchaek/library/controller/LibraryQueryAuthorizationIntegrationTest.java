package com.chaekchaek.library.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.library.domain.Library;
import com.chaekchaek.library.repository.LibraryRepository;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LibraryQueryAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository members;

    @Autowired
    private LibraryRepository libraries;

    @Test
    @DisplayName("실제 보안 필터에서 전체 서재와 책장 상세를 인증 없이 공개한다")
    void should_AllowAnonymousQueries_When_UsingRealSecurityFilterChain() throws Exception {
        // given
        Member member = members.saveAndFlush(Member.create("익명", null, LocalDateTime.now()));
        Library library = libraries.saveAndFlush(Library.create(member.getId(), "책장", Instant.now()));

        // when & then
        mockMvc.perform(get("/api/v1/libraries").param("memberId", member.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libraries[0].libraryId").value(library.getId()));
        mockMvc.perform(get("/api/v1/libraries/{libraryId}", library.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @DisplayName("공개 GET 경로 추가가 같은 경로의 책장 쓰기를 공개하지 않는다")
    void should_KeepWritesProtected_When_ReadPathsArePublic() throws Exception {
        // given

        // when & then
        mockMvc.perform(post("/api/v1/libraries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"책장\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/libraries/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn13\":\"9788925568683\",\"status\":\"READING\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/libraries/1/books/42").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/libraries/order").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"libraryIds\":[]}"))
                .andExpect(status().isUnauthorized());
    }
}
