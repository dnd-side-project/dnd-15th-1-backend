package kr.omong.dulpick.domain.auth.infrastructure.oidc;

public record SocialIdentity(
        String providerSubject,
        String email,
        String tokenNonce
) {
}
