package kr.omong.dulpick.domain.auth.application;

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
        SocialIdentity identity = identity("apple-subject");
        when(appleTokenClient.exchange("authorization-code"))
                .thenReturn(new AppleTokenResponse("access-token", "refresh-token", "id-token"));
        when(identityVerifier.verify("id-token")).thenReturn(identity("apple-subject"));
        when(providerTokenCipher.encrypt("refresh-token")).thenReturn("encrypted-token");

        String encryptedToken = service.exchange("authorization-code", identity);

        assertThat(encryptedToken).isEqualTo("encrypted-token");
    }

    @Test
    void rejectsMismatchedIdentityFromCodeExchange() {
        when(appleTokenClient.exchange("authorization-code"))
                .thenReturn(new AppleTokenResponse("access-token", "refresh-token", "id-token"));
        when(identityVerifier.verify("id-token")).thenReturn(identity("different-subject"));

        assertThatThrownBy(() -> service.exchange(
                "authorization-code",
                identity("apple-subject")
        )).isInstanceOf(AppleAuthorizationException.class);
        verifyNoInteractions(providerTokenCipher);
    }

    private SocialIdentity identity(String subject) {
        return new SocialIdentity(subject, "member@example.com", "nonce");
    }
}
