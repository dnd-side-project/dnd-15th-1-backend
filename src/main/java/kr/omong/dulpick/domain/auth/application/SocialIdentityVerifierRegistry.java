package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.InvalidSocialTokenException;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentityVerifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialIdentityVerifierRegistry {

    private final List<SocialIdentityVerifier> verifiers;

    public SocialIdentityVerifierRegistry(List<SocialIdentityVerifier> verifiers) {
        this.verifiers = verifiers;
    }

    public SocialIdentity verify(SocialProvider provider, String idToken) {
        return verifiers.stream()
                .filter(verifier -> verifier.supports(provider))
                .findFirst()
                .orElseThrow(InvalidSocialTokenException::new)
                .verify(idToken);
    }
}
