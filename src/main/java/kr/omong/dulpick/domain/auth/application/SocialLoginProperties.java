package kr.omong.dulpick.domain.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.social")
public record SocialLoginProperties(
        Duration nonceTtl
) {

    public SocialLoginProperties {
        if (nonceTtl == null || nonceTtl.isZero() || nonceTtl.isNegative()) {
            throw new IllegalArgumentException("auth.social.nonce-ttl must be positive");
        }
    }
}
