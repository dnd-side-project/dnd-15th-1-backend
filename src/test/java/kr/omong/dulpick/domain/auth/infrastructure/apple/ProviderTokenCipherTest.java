package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderTokenCipherTest {

    @Test
    void encryptsAndDecryptsProviderToken() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        ProviderTokenCipher cipher = new ProviderTokenCipher(
                Base64.getEncoder().encodeToString(key),
                new SecureRandom()
        );

        String encrypted = cipher.encrypt("apple-refresh-token");

        assertThat(encrypted).doesNotContain("apple-refresh-token");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("apple-refresh-token");
    }
}
