package kr.omong.dulpick.domain.auth.presentation.controller;

import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.command.result.SocialLoginResult;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final AuthCommandService authCommandService = mock(AuthCommandService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        AuthController controller = new AuthController(authCommandService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void issuesLoginNonce() throws Exception {
        when(authCommandService.issueNonce(SocialProvider.APPLE))
                .thenReturn(new IssuedNonce("nonce", Instant.parse("2026-07-27T00:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/nonce")
                        .contentType("application/json")
                        .content("""
                                {"provider":"APPLE"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").value("nonce"))
                .andExpect(jsonPath("$.expiresAt")
                        .value("2026-07-27T09:00:00"));
    }

    @Test
    void logsInWithVerifiedSocialIdentity() throws Exception {
        IssuedTokens tokens = new IssuedTokens("access", "refresh", 900);
        when(authCommandService.socialLogin(any()))
                .thenReturn(new SocialLoginResult(1L, true, false, tokens));

        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"id-token",
                                  "nonce":"login-nonce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"));
    }

    @Test
    void rejectsBlankIdentityToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"",
                                  "nonce":"login-nonce"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void rejectsBlankNonce() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"id-token",
                                  "nonce":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void returnsMethodNotAllowedForUnsupportedMethod() throws Exception {
        mockMvc.perform(put("/api/v1/auth/nonce")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
