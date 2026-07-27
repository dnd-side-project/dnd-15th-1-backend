package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleTokenClient;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleTokenResponse;
import kr.omong.dulpick.domain.auth.infrastructure.apple.ProviderTokenCipher;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentityVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AppleAuthorizationService {

    private final AppleTokenClient appleTokenClient;
    private final SocialIdentityVerifier appleIdentityVerifier;
    private final ProviderTokenCipher providerTokenCipher;

    public AppleAuthorizationService(
            AppleTokenClient appleTokenClient,
            @Qualifier("appleSocialIdentityVerifier")
            SocialIdentityVerifier appleIdentityVerifier,
            ProviderTokenCipher providerTokenCipher
    ) {
        this.appleTokenClient = appleTokenClient;
        this.appleIdentityVerifier = appleIdentityVerifier;
        this.providerTokenCipher = providerTokenCipher;
    }

    public String exchange(String authorizationCode, SocialIdentity initialIdentity) {
        AppleTokenResponse response = appleTokenClient.exchange(authorizationCode);
        validateResponse(response);
        SocialIdentity exchangedIdentity = appleIdentityVerifier.verify(response.idToken());
        if (!initialIdentity.providerSubject().equals(exchangedIdentity.providerSubject())) {
            throw new AppleAuthorizationException("Apple identity subject mismatch");
        }
        return providerTokenCipher.encrypt(response.refreshToken());
    }

    public void revoke(String encryptedRefreshToken) {
        String refreshToken = providerTokenCipher.decrypt(encryptedRefreshToken);
        appleTokenClient.revoke(refreshToken);
    }

    private void validateResponse(AppleTokenResponse response) {
        if (response == null
                || isBlank(response.refreshToken())
                || isBlank(response.idToken())) {
            throw new AppleAuthorizationException("Apple token response is incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
