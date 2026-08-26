package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsCommand;
import kr.omong.dulpick.domain.notification.application.command.NotificationSettingsService;
import kr.omong.dulpick.domain.notification.domain.MarketingNotificationCampaignRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MarketingNotificationAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private NotificationSettingsService settingsService;

    @Autowired
    private MarketingNotificationCampaignRepository campaignRepository;

    @Autowired
    private MemberNotificationSettingsRepository settingsRepository;

    @Test
    void createsAsyncCampaignForMarketingConsentMembers() throws Exception {
        Member member = createMember();
        Member optedOutMember = createMember();
        settingsService.get(optedOutMember.getId());
        settingsService.update(member.getId(), new NotificationSettingsCommand(
                true,
                true,
                true,
                "2026-08-07"
        ));

        mockMvc.perform(post("/api/v1/admin/notifications/marketing")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"새 소식","body":"둘픽 새 소식을 확인해 주세요."}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.targetCount").value(1))
                .andExpect(jsonPath("$.queuedCount").value(0));

        assertThat(settingsRepository.findById(member.getId())).isPresent();
        assertThat(campaignRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsMarketingCampaignWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/marketing")
                        .with(httpBasic(opsAccessProperties.username(), opsAccessProperties.password()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isForbidden());
    }

    private Member createMember() {
        String subject = "marketing-admin-" + UUID.randomUUID();
        return socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
    }
}
