package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class OidcSocialIdentityVerifier implements SocialIdentityVerifier {

    private final SocialProvider provider;
    private final JwtDecoder jwtDecoder;

    public OidcSocialIdentityVerifier(SocialProvider provider, JwtDecoder jwtDecoder) {
        this.provider = provider;
        this.jwtDecoder = jwtDecoder;
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
                    jwt.getClaimAsString("nonce")
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidSocialTokenException(exception);
        }
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
