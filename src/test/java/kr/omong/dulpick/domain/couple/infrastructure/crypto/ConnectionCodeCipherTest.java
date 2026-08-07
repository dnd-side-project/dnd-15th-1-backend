package kr.omong.dulpick.domain.couple.infrastructure.crypto;

import kr.omong.dulpick.domain.couple.config.ConnectionCodeEncryptionProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionCodeCipherTest {

    @Test
    void encryptsAndDecryptsConnectionCode() {
        ConnectionCodeCipher cipher = new ConnectionCodeCipher(
                key(),
                new SecureRandom()
        );

        String encrypted = cipher.encrypt("ABCDEF");

        assertThat(encrypted).isNotEqualTo("ABCDEF");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("ABCDEF");
    }

    @Test
    void decryptsPreviousVersionAfterKeyRotation() {
        String previousKey = key();
        ConnectionCodeCipher previousCipher = cipher(
                "v1",
                previousKey,
                null,
                null
        );
        String encrypted = previousCipher.encrypt("ABCDEF");
        ConnectionCodeCipher rotatedCipher = cipher(
                "v2",
                key(),
                "v1",
                previousKey
        );

        assertThat(encrypted).startsWith("v1.");
        assertThat(rotatedCipher.decrypt(encrypted)).isEqualTo("ABCDEF");
    }

    @Test
    void decryptsLegacyCiphertextWithPreviousKey() {
        String previousKey = key();
        ConnectionCodeCipher legacyCipher = new ConnectionCodeCipher(
                previousKey,
                new SecureRandom()
        );
        String encrypted = legacyCipher.encrypt("ABCDEF");
        ConnectionCodeCipher rotatedCipher = cipher(
                "v2",
                key(),
                "v1",
                previousKey
        );

        assertThat(encrypted).doesNotContain(".");
        assertThat(rotatedCipher.decrypt(encrypted)).isEqualTo("ABCDEF");
    }

    @Test
    void rejectsCiphertextWithUnknownKeyVersion() {
        ConnectionCodeCipher cipher = cipher("v2", key(), null, null);

        assertThatThrownBy(() -> cipher.decrypt("v1.invalid"))
                .isInstanceOf(ConnectionCodeEncryptionException.class);
    }

    private ConnectionCodeCipher cipher(
            String activeKeyId,
            String activeKey,
            String previousKeyId,
            String previousKey
    ) {
        return new ConnectionCodeCipher(
                new ConnectionCodeEncryptionProperties(
                        activeKeyId,
                        activeKey,
                        previousKeyId,
                        previousKey
                ),
                new SecureRandom()
        );
    }

    private String key() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
