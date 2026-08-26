package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.security.config.OpsAccessProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailAnnouncementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Autowired
    private SocialAccountService socialAccountService;

    @Test
    void previewsRecipientsAndExcludesOptedOutMember() throws Exception {
        String email = "email-notice-" + UUID.randomUUID() + "@example.com";
        long memberId = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                "email-notice-" + UUID.randomUUID(),
                email,
                ProviderAuthorization.none()
        ).member().getId();

        mockMvc.perform(get("/api/v1/admin/email-announcements/preview").with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipients[?(@.email == '" + email + "')]").exists());

        mockMvc.perform(post("/api/v1/admin/email-announcements/opt-outs")
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": %d}".formatted(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true));

        String response = mockMvc.perform(get("/api/v1/admin/email-announcements/preview").with(operator()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain(email);
    }

    @Test
    void sendPersistsAnnouncementInLogOnlyMode() throws Exception {
        mockMvc.perform(post("/api/v1/admin/email-announcements/send")
                        .with(operator())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "개인정보처리방침 변경 안내", "body": "변경 내용을 확인해 주세요."}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED_LOG_ONLY"))
                .andExpect(jsonPath("$.deliveryMode").value("LOG_ONLY"));

        mockMvc.perform(get("/api/v1/admin/email-announcements/history?page=0&size=10").with(operator()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements[0].title").value("개인정보처리방침 변경 안내"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor operator() {
        return httpBasic(opsAccessProperties.username(), opsAccessProperties.password());
    }
}
