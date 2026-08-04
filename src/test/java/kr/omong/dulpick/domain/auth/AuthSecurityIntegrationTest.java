package kr.omong.dulpick.domain.auth;

import com.jayway.jsonpath.JsonPath;
import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.SocialIdentityVerifierRegistry;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthCommandService authCommandService;

    @MockitoBean
    private SocialIdentityVerifierRegistry verifierRegistry;

    @Test
    void allowsNonceIssueWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/nonce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"APPLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").isNotEmpty());
    }

    @Test
    void allowsNonceIssueWithInvalidBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/nonce")
                        .header("Authorization", "Bearer invalid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"APPLE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nonce").isNotEmpty());
    }

    @Test
    void allowsPublicPagesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(
                        "커플을 위한 장소 저장 서비스"
                )));

        mockMvc.perform(get("/privacy"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(
                        "개인정보처리방침"
                )));

        mockMvc.perform(get("/terms"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString(
                        "서비스 이용약관"
                )));
    }

    @Test
    void validatesSocialLoginRequestWithInvalidBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                        .header("Authorization", "Bearer invalid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
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
    void socialLoginSucceedsWithoutTestAuthAccessKey() throws Exception {
        String providerSubject = "provider-member-" + UUID.randomUUID();
        String email = providerSubject + "@example.com";
        MvcResult nonceResult = mockMvc.perform(post("/api/v1/auth/nonce")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"provider":"KAKAO"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String nonce = JsonPath.read(
                nonceResult.getResponse().getContentAsString(),
                "$.nonce"
        );
        when(verifierRegistry.verify(SocialProvider.KAKAO, "provider-id-token"))
                .thenReturn(new SocialIdentity(
                        providerSubject,
                        email,
                        nonce,
                        "kakao-native-app-key"
                ));

        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"KAKAO",
                                  "idToken":"provider-id-token",
                                  "nonce":"%s"
                                }
                                """.formatted(nonce)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").isNumber())
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty());
    }

    @Test
    void reissuesTokensWithInvalidBearerToken() throws Exception {
        Member member = memberRepository.save(Member.create());
        IssuedTokens tokens = tokenService.issue(member);

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .header("Authorization", "Bearer invalid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void rejectsUnknownRefreshTokenAsAuthenticationFailure() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"unknown-refresh-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void protectsLogoutAndGeneralApiWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh-token"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsInvalidBearerTokenOnProtectedApi() throws Exception {
        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer invalid-access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void revokesRefreshTokenWithValidAccessToken() throws Exception {
        Member member = memberRepository.save(Member.create());
        IssuedTokens tokens = tokenService.issue(member);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(tokens.refreshToken())))
                .andExpect(status().isNoContent());

        assertThatThrownBy(() -> authCommandService.reissue(tokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
