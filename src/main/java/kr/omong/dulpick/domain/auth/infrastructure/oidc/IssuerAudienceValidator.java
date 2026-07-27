package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

class IssuerAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_ISSUER = new OAuth2Error(
            "invalid_token",
            "Token issuer is not allowed",
            null
    );
    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token",
            "Token audience is not allowed",
            null
    );

    private final Set<String> issuers;
    private final String audience;

    IssuerAudienceValidator(Set<String> issuers, String audience) {
        this.issuers = issuers;
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getClaimAsString("iss");
        if (issuer == null || !issuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
        }
        if (audience == null || audience.isBlank() || !jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
