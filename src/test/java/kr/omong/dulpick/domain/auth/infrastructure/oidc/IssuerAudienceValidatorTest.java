package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerAudienceValidatorTest {

    private static final String ISSUER = "https://issuer.example";
    private static final String AUDIENCE = "ios-client-id";

    private final IssuerAudienceValidator validator = new IssuerAudienceValidator(
            Set.of(ISSUER),
            AUDIENCE
    );

    @Test
    void acceptsExpectedIssuerAndAudience() {
        assertThat(validator.validate(jwt(ISSUER, AUDIENCE)).hasErrors()).isFalse();
    }

    @Test
    void rejectsUnexpectedAudience() {
        assertThat(validator.validate(jwt(ISSUER, "other-client")).hasErrors()).isTrue();
    }

    @Test
    void rejectsUnexpectedIssuer() {
        assertThat(validator.validate(jwt("https://attacker.example", AUDIENCE)).hasErrors())
                .isTrue();
    }

    private Jwt jwt(String issuer, String audience) {
        Instant issuedAt = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(List.of(audience))
                .subject("provider-subject")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(300))
                .build();
    }
}
