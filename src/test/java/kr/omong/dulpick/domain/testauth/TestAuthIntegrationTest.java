package kr.omong.dulpick.domain.testauth;

import com.jayway.jsonpath.JsonPath;
import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.MemberCommandService;
import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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

    @Autowired
    private AuthCommandService authCommandService;

    @Test
    void signsUpAsKakaoMemberAndUsesAccessTokenOnProtectedApi() throws Exception {
        String email = uniqueEmail("signup");
        AuthTokens tokens = signUp(email);
        TestAuthCredential credential = credentialRepository
                .findByEmail(email)
                .orElseThrow();
        Member member = credential.getMember();
        SocialAccount socialAccount = socialAccountRepository
                .findAllByMemberId(member.getId())
                .getFirst();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.KAKAO);
        assertThat(socialAccount.getEmail()).isEqualTo(email);
        assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD);

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", bearer(tokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(member.getId()))
                .andExpect(jsonPath("$.socialAccounts[0].provider").value("KAKAO"));
    }

    @Test
    void requiresTestAccessKeyAndRejectsDuplicateSignup() throws Exception {
        String email = uniqueEmail("duplicate");
        mockMvc.perform(post("/api/v1/test-auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));

        signUp(email);

        mockMvc.perform(post("/api/v1/test-auth/signup")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void logsInCaseInsensitivelyAsNewMemberAfterWithdrawal() throws Exception {
        String email = uniqueEmail("reactivate");
        signUp(email);
        Long memberId = credentialRepository.findByEmail(email)
                .orElseThrow()
                .getMember()
                .getId();
        memberCommandService.withdraw(memberId);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/test-auth/login")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(not(memberId)))
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.onboardingCompleted").value(false))
                .andReturn();

        assertThat(accessToken(loginResult)).isNotBlank();
        assertThat(memberRepository.findById(memberId).orElseThrow().getStatus())
                .isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(credentialRepository.findByEmail(email).orElseThrow().getMember().getId())
                .isNotEqualTo(memberId);
    }

    @Test
    void returnsCompletedOnboardingStatusOnLogin() throws Exception {
        String email = uniqueEmail("onboarding");
        signUp(email);
        Long memberId = credentialRepository.findByEmail(email)
                .orElseThrow()
                .getMember()
                .getId();
        memberCommandService.initializeProfile(memberId, onboardingProfile());

        mockMvc.perform(post("/api/v1/test-auth/login")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.newMember").value(false))
                .andExpect(jsonPath("$.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.token.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.token.refreshToken").isNotEmpty());
    }

    @Test
    void rejectsIncorrectPasswordWithoutExposingAccountExistence() throws Exception {
        String email = uniqueEmail("wrong-password");
        signUp(email);

        mockMvc.perform(post("/api/v1/test-auth/login")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(
                                email,
                                "incorrect-password"
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rotatesOnlyTestAuthRefreshTokenAndLogsOut() throws Exception {
        AuthTokens tokens = signUp(uniqueEmail("token-flow"));

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

        IssuedTokens replayedTokens = authCommandService.reissue(tokens.refreshToken());
        assertThat(replayedTokens.refreshToken()).isEqualTo(rotatedTokens.refreshToken());

        mockMvc.perform(post("/api/v1/test-auth/logout")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .header("Authorization", bearer(rotatedTokens.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshToken(rotatedTokens.refreshToken())))
                .andExpect(status().isNoContent());

        assertThatThrownBy(() -> authCommandService.reissue(rotatedTokens.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsSocialMemberRefreshToken() throws Exception {
        Member socialMember = memberRepository.save(Member.create(Instant.EPOCH));
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
                        "$.paths['/api/v1/test-auth/signup'].post.responses['201']"
                                + ".content['application/json'].schema['$ref']"
                ).value("#/components/schemas/SocialLoginResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/login'].post.responses['200']"
                                + ".content['application/json'].schema['$ref']"
                ).value("#/components/schemas/SocialLoginResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/reissue'].post.responses['200']"
                                + ".content['application/json'].schema['$ref']"
                ).value("#/components/schemas/TokenResponse"))
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/logout'].post.responses['204']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/logout'].post.security[0].testAuthKey"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/v1/test-auth/logout'].post.security[0].bearerAuth"
                ).exists());
    }

    @Test
    void exposesConcreteExamplesForCommonSwaggerSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.NotificationResponse.properties.type.example")
                        .value("CONTENT_SAVE_MILESTONE"))
                .andExpect(jsonPath("$.components.schemas.PlaceSearchResponse.properties.kakaoPlaceId.example")
                        .value("18699959"))
                .andExpect(jsonPath("$.paths['/api/v1/couples'].post.responses['401'].content['application/json']")
                        .exists());
    }

    @Test
    void exposesExplicitResponsesForDocumentedEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/auth/nonce'].post.responses['400'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/members/me/notification-settings'].put.responses['409'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/push-devices/{deviceId}'].put.responses['503'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/connection-codes/me'].get.responses['409'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/contents/{contentId}'].get.responses['404'].content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/places/{placeId}/contents'].get.responses['404'].content['application/json']")
                        .exists());
    }

    @Test
    void doesNotExposeRetiredMobileEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/places/walking-route']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/places/by-coordinate']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/home/recent-saved-places/all']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/couple-connections/preview']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/recent-searches']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/notifications']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/feedbacks']").doesNotExist());
    }

    @Test
    void exposesExamplesForEveryPrimitiveSwaggerProperty() throws Exception {
        String apiDocs = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Object> document = JsonPath.parse(apiDocs).read("$");
        Map<String, Object> components = castMap(document.get("components"));
        Map<String, Object> schemas = castMap(components.get("schemas"));

        schemas.forEach((schemaName, schema) -> {
            if (!"Pageable".equals(schemaName) && !"Sort".equals(schemaName)) {
                assertPrimitiveExamples(schemaName, castMap(schema));
            }
        });
        Map<String, Object> paths = castMap(document.get("paths"));
        paths.forEach((path, pathItemValue) -> {
            Map<String, Object> pathItem = castMap(pathItemValue);
            List.of("get", "post", "put", "patch", "delete").forEach(method -> {
                Object operationValue = pathItem.get(method);
                if (!(operationValue instanceof Map<?, ?> operation)) {
                    return;
                }
                Object parametersValue = operation.get("parameters");
                if (!(parametersValue instanceof List<?> parameters)) {
                    return;
                }
                parameters.forEach(parameterValue -> {
                    Map<String, Object> parameter = castMap(parameterValue);
                    Map<String, Object> schema = castMap(parameter.get("schema"));
                    assertPrimitiveSchemaExample(path + "[" + method + "]." + parameter.get("name"), schema);
                });
            });
        });
    }

    private void assertPrimitiveExamples(String path, Map<String, Object> schema) {
        Object propertiesValue = schema.get("properties");
        if (propertiesValue instanceof Map<?, ?> properties) {
            properties.forEach((propertyName, propertyValue) -> {
                Map<String, Object> property = castMap(propertyValue);
                assertPrimitiveSchemaExample(path + "." + propertyName, property);
            });
        }
    }

    private void assertPrimitiveSchemaExample(String path, Map<String, Object> schema) {
        String type = schema.get("type") instanceof String value ? value : null;
        if (!schema.containsKey("$ref")
                && type != null
                && !"null".equals(type)
                && !"object".equals(type)) {
            assertThat(schema)
                    .as("Swagger primitive example: %s", path)
                    .containsKey("example");
            assertThat(schema.get("example"))
                    .as("Swagger placeholder example: %s", path)
                    .isNotIn("string", "예시 값");
        }
        Object itemsValue = schema.get("items");
        if (itemsValue instanceof Map<?, ?> items) {
            assertPrimitiveSchemaExample(path + "[]", castMap(items));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private AuthTokens signUp(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/test-auth/signup")
                        .header(TestAuthAccessKeyFilter.HEADER_NAME, ACCESS_KEY)
                        .header("Authorization", "Bearer invalid-token-is-ignored")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newMember").value(true))
                .andExpect(jsonPath("$.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.provider").doesNotExist())
                .andExpect(jsonPath("$.token.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token.expiresIn").isNumber())
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

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private InitializeMemberProfileCommand onboardingProfile() {
        DatePreferences preferences = new DatePreferences(
                DatePreferenceOption.INDOOR,
                DatePreferenceOption.ACTIVE,
                DatePreferenceOption.NIGHT,
                DatePreferenceOption.FOOD
        );
        return new InitializeMemberProfileCommand("둘픽이", 1, preferences);
    }

    private record AuthTokens(
            String accessToken,
            String refreshToken
    ) {
    }
}
