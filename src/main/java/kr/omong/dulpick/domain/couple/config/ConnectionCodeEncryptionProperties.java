package kr.omong.dulpick.domain.couple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("couple.code-encryption")
public record ConnectionCodeEncryptionProperties(
        String activeKeyId,
        String activeKey,
        String previousKeyId,
        String previousKey
) {

    public ConnectionCodeEncryptionProperties {
        boolean hasPreviousKeyId = !isBlank(previousKeyId);
        boolean hasPreviousKey = !isBlank(previousKey);
        if (hasPreviousKeyId != hasPreviousKey) {
            throw new IllegalArgumentException(
                    "previous connection code key id and key must be configured together"
            );
        }
        if (hasPreviousKeyId && activeKeyId.equals(previousKeyId)) {
            throw new IllegalArgumentException(
                    "active and previous connection code key ids must differ"
            );
        }
    }

    public boolean hasPreviousKey() {
        return !isBlank(previousKeyId);
    }

    public ConnectionCodeEncryptionProperties withFallbackKey(String fallbackKey) {
        String resolvedKeyId = isBlank(activeKeyId) ? "v1" : activeKeyId;
        String resolvedKey = isBlank(activeKey) ? fallbackKey : activeKey;
        if (isBlank(resolvedKey)) {
            throw new IllegalArgumentException(
                    "active connection code encryption key must be configured"
            );
        }
        return new ConnectionCodeEncryptionProperties(
                resolvedKeyId,
                resolvedKey,
                previousKeyId,
                previousKey
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
