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

    public ProviderAuthorization exchange(
            String authorizationCode,
            SocialIdentity initialIdentity
    ) {
        String clientId = initialIdentity.audience();
        AppleTokenResponse response = appleTokenClient.exchange(authorizationCode, clientId);
        validateResponse(response);
        SocialIdentity exchangedIdentity = appleIdentityVerifier.verify(response.idToken());
        validateIdentity(initialIdentity, exchangedIdentity);
        return new ProviderAuthorization(
                providerTokenCipher.encrypt(response.refreshToken()),
                clientId
        );
    }

    public void revoke(String encryptedRefreshToken, String clientId) {
        if (isBlank(encryptedRefreshToken) || isBlank(clientId)) {
            throw new AppleAuthorizationException("Apple revocation data is incomplete");
        }
        String refreshToken = providerTokenCipher.decrypt(encryptedRefreshToken);
        appleTokenClient.revoke(refreshToken, clientId);
    }

    private void validateIdentity(
            SocialIdentity initialIdentity,
            SocialIdentity exchangedIdentity
    ) {
        if (!initialIdentity.providerSubject().equals(exchangedIdentity.providerSubject())
                || !initialIdentity.audience().equals(exchangedIdentity.audience())) {
            throw new AppleAuthorizationException("Apple identity mismatch");
        }
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
