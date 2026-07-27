package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("auth.apple")
public record AppleTokenProperties(
        String teamId,
        String keyId,
        String clientId,
        String privateKeyBase64,
        String tokenEncryptionKey,
        String tokenUri,
        String revokeUri,
        Duration clientSecretTtl
) {
}
