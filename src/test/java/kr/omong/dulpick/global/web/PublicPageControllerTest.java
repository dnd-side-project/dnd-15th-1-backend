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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
    void downloadRedirectsToAppStore() throws Exception {
        mockMvc.perform(get("/download"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(APP_STORE_URL));
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
        Resource versionOne = controller.privacyHistoryV1();

        assertThat(history.getFilename()).isEqualTo("privacy-history.html");
        assertThat(versionOne.getFilename()).isEqualTo("privacy-v1.0.html");
    }

}
