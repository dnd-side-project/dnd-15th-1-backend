package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Set;

public class OidcSocialIdentityVerifier implements SocialIdentityVerifier {

    private final SocialProvider provider;
    private final JwtDecoder jwtDecoder;
    private final Set<String> allowedAudiences;

    public OidcSocialIdentityVerifier(
            SocialProvider provider,
            JwtDecoder jwtDecoder,
            Set<String> allowedAudiences
    ) {
        this.provider = provider;
        this.jwtDecoder = jwtDecoder;
        this.allowedAudiences = Set.copyOf(allowedAudiences);
    }

    @Override
    public boolean supports(SocialProvider provider) {
        return this.provider == provider;
    }

    @Override
    public SocialIdentity verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            return new SocialIdentity(
                    requiredSubject(jwt),
                    verifiedEmail(jwt),
                    jwt.getClaimAsString("nonce"),
                    requiredAudience(jwt)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidSocialTokenException(exception);
        }
    }

    private String requiredAudience(Jwt jwt) {
        return jwt.getAudience().stream()
                .filter(allowedAudiences::contains)
                .findFirst()
                .orElseThrow(InvalidSocialTokenException::new);
    }

    private String requiredSubject(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidSocialTokenException();
        }
        return subject;
    }

    private String verifiedEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (provider == SocialProvider.KAKAO || isEmailVerified(jwt)) {
            return email;
        }
        return null;
    }

    private boolean isEmailVerified(Jwt jwt) {
        Object claim = jwt.getClaim("email_verified");
        return Boolean.TRUE.equals(claim) || "true".equalsIgnoreCase(String.valueOf(claim));
    }
}
