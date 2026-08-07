package kr.omong.dulpick.domain.couple.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("couple")
public record CoupleProperties(
        String publicBaseUrl,
        String codeEncryptionKey
) {
}
