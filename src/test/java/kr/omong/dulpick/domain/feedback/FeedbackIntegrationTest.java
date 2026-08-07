package kr.omong.dulpick.domain.feedback;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.feedback.domain.MemberFeedbackRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberFeedbackRepository feedbackRepository;

    @Test
    void receivesFeedbackIdempotently() throws Exception {
        IssuedTokens tokens = createTokens();
        UUID clientRequestId = UUID.randomUUID();
        String request = request(clientRequestId, "FEATURE_SUGGESTION", "  필터가 필요해요.  ");

        mockMvc.perform(post("/api/v1/feedbacks")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feedbackId").isNumber())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        mockMvc.perform(post("/api/v1/feedbacks")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        assertThat(feedbackRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidAndUnauthenticatedFeedback() throws Exception {
        IssuedTokens tokens = createTokens();

        mockMvc.perform(post("/api/v1/feedbacks")
                        .header("Authorization", bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(UUID.randomUUID(), "UNKNOWN", "내용")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        mockMvc.perform(post("/api/v1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(UUID.randomUUID(), "INQUIRY", "내용")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesTermsAndPrivacyWithoutAuthentication() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/terms"))
                .andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/privacy"))
                .andExpect(status().isOk());
    }

    private IssuedTokens createTokens() {
        String subject = "feedback-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        return tokenService.issue(member);
    }

    private String request(UUID clientRequestId, String type, String content) {
        return """
                {
                  "clientRequestId":"%s",
                  "type":"%s",
                  "content":"%s"
                }
                """.formatted(clientRequestId, type, content);
    }

    private String bearer(IssuedTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }
}
