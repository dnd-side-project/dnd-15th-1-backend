package kr.omong.dulpick.global.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UniversalLinkIntegrationTest {

    private static final String AASA_PATH =
            "/.well-known/apple-app-site-association";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesAppleAppSiteAssociationWithoutAuthenticationOrRedirect() throws Exception {
        mockMvc.perform(get(AASA_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(
                        "$.applinks.details[0].appIDs[*]",
                        containsInAnyOrder(
                                "DZAU4TTS8S.com.dulpick.app",
                                "DZAU4TTS8S.com.dulpick.dev"
                        )
                ))
                .andExpect(jsonPath(
                        "$.applinks.details[0].components[0]['/']"
                ).value("/connect"));
    }

    @Test
    void servesPublicConnectionFallbackPage() throws Exception {
        mockMvc.perform(get("/connect")
                        .queryParam("code", "ABCDE")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andExpect(content().string(containsString("커플 연결 초대")))
                .andExpect(content().string(containsString("둘픽 커플연결 초대코드가 도착했어요")))
                .andExpect(content().string(containsString("apps.apple.com/kr/app")))
                .andExpect(content().string(containsString("textContent")))
                .andExpect(content().string(not(containsString("innerHTML"))));
    }

    @Test
    void servesAppStoreDownloadLinkOnHomePage() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("App Store에서 둘픽 다운로드")))
                .andExpect(content().string(containsString("id6796011877")))
                .andExpect(content().string(not(containsString("iOS 앱 출시 준비 중"))));
    }

    @Test
    void doesNotReflectInvalidConnectionCodeInHtml() throws Exception {
        String maliciousCode = "<script>alert('xss')</script>";

        mockMvc.perform(get("/connect")
                        .queryParam("code", maliciousCode)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(maliciousCode))));
    }

    @Test
    void keepsApplicationApiProtected() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }
}
