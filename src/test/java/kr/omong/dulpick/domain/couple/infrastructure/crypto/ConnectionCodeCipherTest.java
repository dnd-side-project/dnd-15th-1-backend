package kr.omong.dulpick.domain.couple.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionCodeCipherTest {

    @Test
    void encryptsAndDecryptsConnectionCode() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        ConnectionCodeCipher cipher = new ConnectionCodeCipher(
                Base64.getEncoder().encodeToString(key),
                new SecureRandom()
        );

        String encrypted = cipher.encrypt("ABCDEF");

        assertThat(encrypted).isNotEqualTo("ABCDEF");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("ABCDEF");
    }
}
