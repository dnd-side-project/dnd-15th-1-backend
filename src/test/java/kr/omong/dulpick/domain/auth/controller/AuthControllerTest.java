package kr.omong.dulpick.domain.auth.controller;

import kr.omong.dulpick.domain.auth.application.IssuedNonce;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.LoginNonceService;
import kr.omong.dulpick.domain.auth.application.SocialLoginResult;
import kr.omong.dulpick.domain.auth.application.SocialLoginService;
import kr.omong.dulpick.domain.auth.application.TokenService;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.global.error.GlobalExceptionHandler;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final LoginNonceService loginNonceService = mock(LoginNonceService.class);
    private final SocialLoginService socialLoginService = mock(SocialLoginService.class);
    private final TokenService tokenService = mock(TokenService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        AuthController controller = new AuthController(
                loginNonceService,
                socialLoginService,
                tokenService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void issuesLoginNonce() throws Exception {
        when(loginNonceService.issue(SocialProvider.APPLE))
                .thenReturn(new IssuedNonce("nonce", Instant.parse("2026-07-27T00:00:00Z")));

        mockMvc.perform(post("/api/v1/auth/nonce")
                        .contentType("application/json")
                        .content("""
                                {"provider":"APPLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").value("nonce"));
    }

    @Test
    void logsInWithVerifiedSocialIdentity() throws Exception {
        IssuedTokens tokens = new IssuedTokens("access", "refresh", 900);
        when(socialLoginService.login(any()))
                .thenReturn(new SocialLoginResult(1L, true, tokens));

        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"id-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"));
    }

    @Test
    void rejectsBlankIdentityToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
