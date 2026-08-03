package kr.omong.dulpick.domain.auth.application.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SocialLoginPropertiesTest {

    @Test
    void acceptsPositiveNonceTtl() {
        SocialLoginProperties properties = new SocialLoginProperties(Duration.ofMinutes(10));

        assertThat(properties.nonceTtl()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void rejectsMissingOrNonPositiveNonceTtl() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SocialLoginProperties(null))
                .withMessageContaining("auth.social.nonce-ttl");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SocialLoginProperties(Duration.ZERO))
                .withMessageContaining("auth.social.nonce-ttl");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SocialLoginProperties(Duration.ofSeconds(-1)))
                .withMessageContaining("auth.social.nonce-ttl");
    }
}
