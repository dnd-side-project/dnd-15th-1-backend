package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class RequiredExpirationValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error MISSING_EXPIRATION = new OAuth2Error(
            "invalid_token",
            "Token expiration is required",
            null
    );

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getExpiresAt() == null) {
            return OAuth2TokenValidatorResult.failure(MISSING_EXPIRATION);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
