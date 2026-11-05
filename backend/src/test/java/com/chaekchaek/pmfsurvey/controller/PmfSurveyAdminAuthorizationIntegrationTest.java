package com.chaekchaek.pmfsurvey.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chaekchaek.actor.domain.Actor;
import com.chaekchaek.actor.repository.ActorRepository;
import com.chaekchaek.auth.token.access.AccessTokenProvider;
import com.chaekchaek.member.domain.Member;
import com.chaekchaek.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PmfSurveyAdminAuthorizationIntegrationTest {

    private static final String SUMMARY_PATH = "/api/v1/admin/pmf-survey/responses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private AccessTokenProvider accessTokenProvider;

    @Test
    @DisplayName("인증 없이 PMF 설문 집계를 조회하면 거절한다")
    void should_RejectSummary_When_ActorIsUnauthenticated() throws Exception {
        mockMvc.perform(get(SUMMARY_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("일반 회원의 PMF 설문 집계 조회를 거절한다")
    void should_RejectSummary_When_ActorIsNotAdmin() throws Exception {
        mockMvc.perform(get(SUMMARY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("관리자는 PMF 설문 집계를 조회한다")
    void should_ReturnSummary_When_ActorIsAdmin() throws Exception {
        mockMvc.perform(get(SUMMARY_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respondentCount").value(0))
                .andExpect(jsonPath("$.questions").isArray());
    }

    private String accessToken(boolean admin) {
        LocalDateTime now = LocalDateTime.now();
        Member member = memberRepository.save(Member.create("PMF " + UUID.randomUUID(), null, now));
        Actor actor = Actor.member(member, now);
        if (admin) {
            actor.grantAdmin();
        }
        actorRepository.save(actor);
        return accessTokenProvider.issue(member);
    }
}
