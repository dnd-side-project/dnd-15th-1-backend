package kr.omong.dulpick.domain.auth.application.support;

import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;

import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleTokenClient;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleTokenResponse;
import kr.omong.dulpick.domain.auth.infrastructure.apple.ProviderTokenCipher;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentity;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.SocialIdentityVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppleAuthorizationServiceTest {

    private final AppleTokenClient appleTokenClient = mock(AppleTokenClient.class);
    private final SocialIdentityVerifier identityVerifier = mock(SocialIdentityVerifier.class);
    private final ProviderTokenCipher providerTokenCipher = mock(ProviderTokenCipher.class);
    private final AppleAuthorizationService service = new AppleAuthorizationService(
            appleTokenClient,
            identityVerifier,
            providerTokenCipher
    );

    @Test
    void exchangesCodeAndEncryptsRefreshToken() {
        SocialIdentity identity = identity("apple-subject", "com.dulpick.app");
        when(appleTokenClient.exchange("authorization-code", "com.dulpick.app"))
                .thenReturn(new AppleTokenResponse("access-token", "refresh-token", "id-token"));
        when(identityVerifier.verify("id-token"))
                .thenReturn(identity("apple-subject", "com.dulpick.app"));
        when(providerTokenCipher.encrypt("refresh-token")).thenReturn("encrypted-token");

        ProviderAuthorization authorization = service.exchange("authorization-code", identity);

        assertThat(authorization.encryptedRefreshToken()).isEqualTo("encrypted-token");
        assertThat(authorization.clientId()).isEqualTo("com.dulpick.app");
    }

    @Test
    void usesDevClientIdFromVerifiedIdentityForCodeExchange() {
        SocialIdentity identity = identity("apple-subject", "com.dulpick.dev");
        when(appleTokenClient.exchange("authorization-code", "com.dulpick.dev"))
                .thenReturn(new AppleTokenResponse(
                        "access-token",
                        "refresh-token",
                        "id-token"
                ));
        when(identityVerifier.verify("id-token")).thenReturn(identity);
        when(providerTokenCipher.encrypt("refresh-token")).thenReturn("encrypted-token");

        ProviderAuthorization authorization = service.exchange("authorization-code", identity);

        assertThat(authorization.clientId()).isEqualTo("com.dulpick.dev");
    }

    @Test
    void rejectsMismatchedIdentityFromCodeExchange() {
        when(appleTokenClient.exchange("authorization-code", "com.dulpick.app"))
                .thenReturn(new AppleTokenResponse("access-token", "refresh-token", "id-token"));
        when(identityVerifier.verify("id-token"))
                .thenReturn(identity("different-subject", "com.dulpick.app"));

        assertThatThrownBy(() -> service.exchange(
                "authorization-code",
                identity("apple-subject", "com.dulpick.app")
        )).isInstanceOf(AppleAuthorizationException.class);
        verifyNoInteractions(providerTokenCipher);
    }

    @Test
    void rejectsMismatchedAudienceFromCodeExchange() {
        when(appleTokenClient.exchange("authorization-code", "com.dulpick.app"))
                .thenReturn(new AppleTokenResponse(
                        "access-token",
                        "refresh-token",
                        "id-token"
                ));
        when(identityVerifier.verify("id-token"))
                .thenReturn(identity("apple-subject", "com.dulpick.dev"));

        assertThatThrownBy(() -> service.exchange(
                "authorization-code",
                identity("apple-subject", "com.dulpick.app")
        )).isInstanceOf(AppleAuthorizationException.class);
        verifyNoInteractions(providerTokenCipher);
    }

    @Test
    void decryptsAndRevokesWithStoredClientId() {
        when(providerTokenCipher.decrypt("encrypted-token")).thenReturn("refresh-token");

        service.revoke("encrypted-token", "com.dulpick.dev");

        verify(appleTokenClient).revoke("refresh-token", "com.dulpick.dev");
    }

    private SocialIdentity identity(String subject, String audience) {
        return new SocialIdentity(
                subject,
                "member@example.com",
                "nonce",
                audience
        );
    }
}
