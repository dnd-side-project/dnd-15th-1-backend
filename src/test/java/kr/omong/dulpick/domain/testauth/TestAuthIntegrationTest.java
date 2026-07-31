package kr.omong.dulpick.domain.testauth;

import com.jayway.jsonpath.JsonPath;
import kr.omong.dulpick.domain.auth.application.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.TokenService;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.MemberStatus;
import kr.omong.dulpick.domain.testauth.domain.TestAuthCredential;
import kr.omong.dulpick.domain.testauth.domain.TestAuthCredentialRepository;
import kr.omong.dulpick.domain.testauth.security.TestAuthAccessKeyFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "features.test-auth.enabled=true",
        "features.test-auth.access-key=0123456789abcdef0123456789abcdef",
        "springdoc.api-docs.enabled=true"
})
@AutoConfigureMockMvc
@Transactional
class TestAuthIntegrationTest {

    private static final String ACCESS_KEY = "0123456789abcdef0123456789abcdef";
    private static final String PASSWORD = "test-password-1234";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAuthCredentialRepository credentialRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberCommandService memberCommandService;

    @Autowired
    private TokenService tokenService;

    @Test
    void signsUpAsKakaoMemberAndUsesAccessTokenOnProtectedApi() throws Exception {
        AuthTokens tokens = signUp("swagger-test@example.com");
        TestAuthCredential credential = credentialRepository
                .findByEmail("swagger-test@example.com")
                .orElseThrow();
        Member member = credential.getMember();
        SocialAccount socialAccount = socialAccountRepository
                .findAllByMemberId(member.getId())
                .getFirst();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialAccount.getEmail()).isEqualTo("swagger-test@example.com");
        assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD);

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.socialAccounts[0].provider").value("KAKAO"));
    }

    @Test
    void requiresTestAccessKeyAndRejectsDuplicateSignup() throws Exception {
        mockMvc.perform(post("/api/v1/test-auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("duplicate@example.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        signUp("duplicate@example.com");

        mockMvc.perform(post("/api/v1/test-auth/signup")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("DUPLICATE@example.com", PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void logsInCaseInsensitivelyAndReactivatesWithdrawnMember() throws Exception {
        signUp("reactivate@example.com");
        Long memberId = credentialRepository.findByEmail("reactivate@example.com")
                .orElseThrow()
                .getMember()
                .getId();
        memberCommandService.withdraw(memberId);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/test-auth/login")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("REACTIVATE@example.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andReturn();

        assertThat(accessToken(loginResult)).isNotBlank();
        assertThat(memberRepository.findById(memberId).orElseThrow().getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void rejectsIncorrectPasswordWithoutExposingAccountExistence() throws Exception {
        signUp("wrong-password@example.com");

        mockMvc.perform(post("/api/v1/test-auth/login")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(
                                "wrong-password@example.com",
                                "incorrect-password"
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rotatesOnlyTestAuthRefreshTokenAndLogsOut() throws Exception {
        AuthTokens tokens = signUp("token-flow@example.com");

        MvcResult reissueResult = mockMvc.perform(post("/api/v1/test-auth/reissue")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .header("Authorization", "Bearer invalid-token-is-ignored")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken(tokens.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        AuthTokens rotatedTokens = tokenResponse(reissueResult);

        assertThatThrownBy(() -> tokenService.rotate(tokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);

        mockMvc.perform(post("/api/v1/test-auth/logout")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .header("Authorization", bearer(rotatedTokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken(rotatedTokens.refreshToken())))
                .andExpect(status().isNoContent());

        assertThatThrownBy(() -> tokenService.rotate(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsSocialMemberRefreshToken() throws Exception {
        Member socialMember = memberRepository.save(Member.create());
        IssuedTokens socialTokens = tokenService.issue(socialMember);

        mockMvc.perform(post("/api/v1/test-auth/reissue")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken(socialTokens.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void exposesAuthenticationTwoSwaggerCategoryAndSecuritySchemes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[*].name", hasItem("인증2")))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.testAuthKey.name"
                ).value(TestAuthAccessKeyFilter.HEADER_NAME))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/signup'].post"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/signup'].post.security[0].testAuthKey"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/signup'].post.responses['401']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/logout'].post.security[0].testAuthKey"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/logout'].post.security[0].bearerAuth"
                ).exists());
    }

    private AuthTokens signUp(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/test-auth/signup")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .header("Authorization", "Bearer invalid-token-is-ignored")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("KAKAO"))
                .andReturn();
        return nestedTokenResponse(result);
    }

    private AuthTokens nestedTokenResponse(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        return new AuthTokens(
                JsonPath.read(response, "$.token.accessToken"),
                JsonPath.read(response, "$.token.refreshToken")
        );
    }

    private AuthTokens tokenResponse(MvcResult result) throws Exception {
        String response = result.getResponse().getContentAsString();
        return new AuthTokens(
                JsonPath.read(response, "$.accessToken"),
                JsonPath.read(response, "$.refreshToken")
        );
    }

    private String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token.accessToken"
        );
    }

    private String credentials(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String refreshToken(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record AuthTokens(
            String accessToken,
            String refreshToken
    ) {
    }
}
