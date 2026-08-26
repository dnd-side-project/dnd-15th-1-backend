package kr.omong.dulpick.global.security;

import jakarta.servlet.http.Cookie;
import kr.omong.dulpick.global.security.config.OpsAccessProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class OpsAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpsAccessProperties opsAccessProperties;

    @Test
    void authenticatesOperatorWithLoginFormAndCsrfCookie() throws Exception {
        MvcResult loginPage = mockMvc.perform(get("/ops/login").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andReturn();
        Cookie csrfCookie = loginPage.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/ops/login")
                        .cookie(csrfCookie)
                        .param("username", opsAccessProperties.username())
                        .param("password", opsAccessProperties.password())
                        .param("_csrf", csrfCookie.getValue())
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ops/places"));
    }
}
