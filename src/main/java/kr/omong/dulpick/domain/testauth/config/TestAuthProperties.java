package kr.omong.dulpick.domain.testauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("features.test-auth")
public record TestAuthProperties(
        boolean enabled,
        String accessKey
) {
}
