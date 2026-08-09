package kr.omong.dulpick.global.security.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSha256Test {

    @Test
    void createsStableKeyedDigestWithoutExposingSource() {
        String first = HmacSha256.hex("first-secret-key", "192.0.2.1");
        String repeated = HmacSha256.hex("first-secret-key", "192.0.2.1");
        String differentKey = HmacSha256.hex("second-secret-key", "192.0.2.1");

        assertThat(first).hasSize(64).isEqualTo(repeated);
        assertThat(first).isNotEqualTo(differentKey).doesNotContain("192.0.2.1");
    }
}
