package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;

public interface SocialIdentityVerifier {

    boolean supports(SocialProvider provider);

    SocialIdentity verify(String idToken);
}
