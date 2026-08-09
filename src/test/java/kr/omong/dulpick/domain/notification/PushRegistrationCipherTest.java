package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.notification.config.PushProperties;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationEncryptionException;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushRegistrationCipherTest {

    @Test
    void rejectsMalformedConfiguredKeyDuringConstruction() {
        assertThatThrownBy(() -> new PushRegistrationCipher(
                new PushProperties("not-base64"),
                new SecureRandom()
        )).isInstanceOf(PushRegistrationEncryptionException.class);
    }

    @Test
    void rejectsMissingKeyWhenFcmConfigurationRequiresIt() {
        PushRegistrationCipher cipher = new PushRegistrationCipher(
                new PushProperties(""),
                new SecureRandom()
        );

        assertThatIllegalStateException()
                .isThrownBy(cipher::requireConfigured)
                .withMessage("PUSH_REGISTRATION_ENCRYPTION_KEY is required");
    }
}
