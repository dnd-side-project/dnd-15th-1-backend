package kr.omong.dulpick.global.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
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
}
