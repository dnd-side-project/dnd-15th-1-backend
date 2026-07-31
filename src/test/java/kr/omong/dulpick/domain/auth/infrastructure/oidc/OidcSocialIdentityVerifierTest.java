package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OidcSocialIdentityVerifierTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);

    @Test
    void extractsVerifiedIdentityFromGoogleToken() {
        OidcSocialIdentityVerifier verifier = new OidcSocialIdentityVerifier(
                SocialProvider.GOOGLE,
                jwtDecoder,
                Set.of("google-client-id")
        );
        when(jwtDecoder.decode("id-token")).thenReturn(jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", true,
                "nonce", "login-nonce",
                "aud", List.of("google-client-id")
        )));

        SocialIdentity identity = verifier.verify("id-token");

        assertThat(identity.providerSubject()).isEqualTo("google-subject");
        assertThat(identity.email()).isEqualTo("member@example.com");
        assertThat(identity.tokenNonce()).isEqualTo("login-nonce");
        assertThat(identity.audience()).isEqualTo("google-client-id");
    }

    @Test
    void ignoresUnverifiedGoogleEmail() {
        OidcSocialIdentityVerifier verifier = new OidcSocialIdentityVerifier(
                SocialProvider.GOOGLE,
                jwtDecoder,
                Set.of("google-client-id")
        );
        when(jwtDecoder.decode("id-token")).thenReturn(jwt(Map.of(
                "sub", "google-subject",
                "email", "member@example.com",
                "email_verified", false,
                "aud", List.of("google-client-id")
        )));

        assertThat(verifier.verify("id-token").email()).isNull();
    }

    @Test
    void extractsKakaoIdentityAndNonce() {
        OidcSocialIdentityVerifier verifier = new OidcSocialIdentityVerifier(
                SocialProvider.KAKAO,
                jwtDecoder,
                Set.of("kakao-client-id")
        );
        when(jwtDecoder.decode("id-token")).thenReturn(jwt(Map.of(
                "sub", "kakao-subject",
                "email", "member@example.com",
                "nonce", "login-nonce",
                "aud", List.of("kakao-client-id")
        )));

        SocialIdentity identity = verifier.verify("id-token");

        assertThat(identity.providerSubject()).isEqualTo("kakao-subject");
        assertThat(identity.email()).isEqualTo("member@example.com");
        assertThat(identity.tokenNonce()).isEqualTo("login-nonce");
    }

    private Jwt jwt(Map<String, Object> claims) {
        Instant issuedAt = Instant.now();
        return new Jwt(
                "id-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("alg", "RS256"),
                claims
        );
    }
}
