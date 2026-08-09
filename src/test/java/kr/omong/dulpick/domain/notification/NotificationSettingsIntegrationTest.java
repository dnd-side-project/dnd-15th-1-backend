package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.notification.domain.MarketingConsentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationSettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MarketingConsentHistoryRepository consentHistoryRepository;

    @Test
    void createsDefaultsAndUpdatesAllSettings() throws Exception {
        TestMember member = createMember();

        mockMvc.perform(get("/api/v1/members/me/notification-settings")
                        .header("Authorization", bearer(member.tokens())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentSavedEnabled").value(true))
                .andExpect(jsonPath("$.dateScheduleEnabled").value(true))
                .andExpect(jsonPath("$.marketingEnabled").value(false))
                .andExpect(jsonPath("$.marketingConsentVersion").doesNotExist())
                .andExpect(jsonPath("$.availableMarketingConsentVersion")
                        .value("2026-08-07"));

        mockMvc.perform(put("/api/v1/members/me/notification-settings")
                        .header("Authorization", bearer(member.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentSavedEnabled":false,
                                  "dateScheduleEnabled":false,
                                  "marketingEnabled":true,
                                  "marketingConsentVersion":"2026-08-07"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentSavedEnabled").value(false))
                .andExpect(jsonPath("$.dateScheduleEnabled").value(false))
                .andExpect(jsonPath("$.marketingEnabled").value(true))
                .andExpect(jsonPath("$.marketingConsentVersion").value("2026-08-07"))
                .andExpect(jsonPath("$.availableMarketingConsentVersion")
                        .value("2026-08-07"));

        assertThat(consentHistoryRepository.countByMemberId(member.memberId())).isEqualTo(1);
    }

    @Test
    void requiresCurrentConsentVersionWhenEnablingMarketing() throws Exception {
        TestMember member = createMember();

        mockMvc.perform(put("/api/v1/members/me/notification-settings")
                        .header("Authorization", bearer(member.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentSavedEnabled":true,
                                  "dateScheduleEnabled":true,
                                  "marketingEnabled":true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("MARKETING_CONSENT_VERSION_REQUIRED"));

        mockMvc.perform(put("/api/v1/members/me/notification-settings")
                        .header("Authorization", bearer(member.tokens()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contentSavedEnabled":true,
                                  "dateScheduleEnabled":true,
                                  "marketingEnabled":true,
                                  "marketingConsentVersion":"old"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("MARKETING_CONSENT_VERSION_OUTDATED"));
    }

    private TestMember createMember() {
        String subject = "notification-settings-" + UUID.randomUUID();
        Member member = socialAccountService.getOrCreate(
                SocialProvider.KAKAO,
                subject,
                subject + "@example.com",
                ProviderAuthorization.none()
        ).member();
        return new TestMember(member.getId(), tokenService.issue(member));
    }

    private String bearer(IssuedTokens tokens) {
        return "Bearer " + tokens.accessToken();
    }

    private record TestMember(Long memberId, IssuedTokens tokens) {
    }
}
