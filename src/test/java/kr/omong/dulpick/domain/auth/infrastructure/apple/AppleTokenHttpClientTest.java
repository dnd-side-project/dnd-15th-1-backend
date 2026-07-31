package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleTokenHttpClientTest {

    private static final String TOKEN_URI = "https://apple.example/auth/token";
    private static final String REVOKE_URI = "https://apple.example/auth/revoke";

    private final AppleClientSecretGenerator clientSecretGenerator =
            mock(AppleClientSecretGenerator.class);

    private MockRestServiceServer server;
    private AppleTokenHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AppleTokenProperties properties = new AppleTokenProperties(
                "TEAM_ID",
                "KEY_ID",
                Set.of("com.dulpick.app", "com.dulpick.dev"),
                "/unused/test/key.p8",
                "",
                TOKEN_URI,
                REVOKE_URI,
                Duration.ofMinutes(5)
        );
        client = new AppleTokenHttpClient(
                properties,
                clientSecretGenerator,
                builder.build()
        );
    }

    @Test
    void exchangesAuthorizationCodeWithProdClientId() {
        when(clientSecretGenerator.generate("com.dulpick.app"))
                .thenReturn("prod-client-secret");
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("client_id=com.dulpick.app"),
                        containsString("client_secret=prod-client-secret"),
                        containsString("grant_type=authorization_code"),
                        containsString("code=authorization-code")
                )))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "id_token": "id-token"
                        }
                        """, MediaType.APPLICATION_JSON));

        AppleTokenResponse response = client.exchange(
                "authorization-code",
                "com.dulpick.app"
        );

        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        server.verify();
    }

    @Test
    void exchangesAuthorizationCodeWithDevClientId() {
        when(clientSecretGenerator.generate("com.dulpick.dev"))
                .thenReturn("dev-client-secret");
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(content().string(containsString(
                        "client_id=com.dulpick.dev"
                )))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "id_token": "id-token"
                        }
                        """, MediaType.APPLICATION_JSON));

        client.exchange("authorization-code", "com.dulpick.dev");

        server.verify();
    }

    @Test
    void revokesRefreshTokenWithStoredClientId() {
        when(clientSecretGenerator.generate("com.dulpick.dev"))
                .thenReturn("dev-client-secret");
        server.expect(once(), requestTo(REVOKE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("client_id=com.dulpick.dev"),
                        containsString("token=refresh-token"),
                        containsString("token_type_hint=refresh_token")
                )))
                .andRespond(withSuccess());

        client.revoke("refresh-token", "com.dulpick.dev");

        server.verify();
    }

    @Test
    void sanitizesTokenEndpointFailure() {
        when(clientSecretGenerator.generate("com.dulpick.app"))
                .thenReturn("sensitive-client-secret");
        server.expect(once(), requestTo(TOKEN_URI))
                .andRespond(withBadRequest().body("""
                        {"error":"invalid_grant","token":"sensitive-authorization-code"}
                        """));

        assertThatThrownBy(() -> client.exchange(
                "sensitive-authorization-code",
                "com.dulpick.app"
                )).isInstanceOf(AppleAuthorizationException.class)
                .hasMessage("Apple authorization code exchange failed")
                .hasMessageNotContaining("sensitive-authorization-code")
                .hasMessageNotContaining("sensitive-client-secret")
                .hasMessageNotContaining("invalid_grant");
        server.verify();
    }
}
