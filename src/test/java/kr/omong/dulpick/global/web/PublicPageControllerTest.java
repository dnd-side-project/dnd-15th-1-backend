package kr.omong.dulpick.global.web;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicPageControllerTest {

    private static final String APP_STORE_URL =
            "https://apps.apple.com/kr/app/%EB%91%98%ED%94%BD-dulpick/id6796011877";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PublicPageController(mock(CsrfTokenRepository.class)))
                .build();
    }

    @Test
    void downloadServesTrackedDownloadPage() throws Exception {
        mockMvc.perform(get("/download"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("G-91V1WZ1N46")))
                .andExpect(content().string(containsString(APP_STORE_URL)));
    }

    @Test
    void downloadPublishesAnalyticsEventWhenPublisherIsAvailable() {
        var publisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        var controller = new PublicPageController(
                mock(CsrfTokenRepository.class),
                publisher,
                Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC)
        );

        controller.download();

        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.<Object>argThat(
                event -> event instanceof AnalyticsActionEvent action
                        && action.eventType().name().equals("DOWNLOAD_PAGE_VISITED")
                        && action.occurredAt().equals(Instant.parse("2026-09-10T00:00:00Z"))
        ));
    }

    @Test
    void exposesPrivacyPolicyHistoryPages() throws Exception {
        PublicPageController controller = new PublicPageController(mock(CsrfTokenRepository.class));

        Resource history = controller.privacyHistory();
        Resource versionOne = controller.privacyHistoryV1_0();
        Resource versionOnePointOne = controller.privacyHistoryV1_1();

        assertThat(history.getFilename()).isEqualTo("privacy-history.html");
        assertThat(versionOne.getFilename()).isEqualTo("privacy-v1.0.html");
        assertThat(versionOnePointOne.getFilename()).isEqualTo("privacy-v1.1.html");
    }

    @Test
    void exposesTermsAndMarketingHistoryPages() {
        PublicPageController controller = new PublicPageController(mock(CsrfTokenRepository.class));

        assertThat(controller.termsHistory().getFilename()).isEqualTo("terms-history.html");
        assertThat(controller.termsHistoryV1_0().getFilename()).isEqualTo("terms-v1.0.html");
        assertThat(controller.marketingHistory().getFilename()).isEqualTo("marketing-history.html");
        assertThat(controller.marketingHistoryV1_0().getFilename()).isEqualTo("marketing-v1.0.html");
    }

    @Test
    void servesLegalVersionHistoryPages() throws Exception {
        mockMvc.perform(get("/privacy/history/v1.1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("버전 1.1")));
        mockMvc.perform(get("/terms/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("서비스 이용약관 버전 이력")));
        mockMvc.perform(get("/marketing/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("마케팅 알림 약관 버전 이력")));
    }

}
