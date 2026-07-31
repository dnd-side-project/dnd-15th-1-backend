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
    private final Set<String> audiences;

    IssuerAudienceValidator(Set<String> issuers, Set<String> audiences) {
        this.issuers = issuers;
        this.audiences = audiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String issuer = jwt.getClaimAsString("iss");
        if (issuer == null || !issuers.contains(issuer)) {
            return OAuth2TokenValidatorResult.failure(INVALID_ISSUER);
        }
        if (audiences == null
                || audiences.isEmpty()
                || jwt.getAudience().stream().noneMatch(audiences::contains)) {
            return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
