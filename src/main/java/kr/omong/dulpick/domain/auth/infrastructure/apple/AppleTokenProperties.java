package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

@ConfigurationProperties("auth.apple")
public record AppleTokenProperties(
        String teamId,
        String keyId,
        Set<String> clientIds,
        String privateKeyPath,
        String tokenEncryptionKey,
        String tokenUri,
        String revokeUri,
        Duration clientSecretTtl
) {
}
