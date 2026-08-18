package kr.omong.dulpick.global.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ops")
public record OpsAccessProperties(
        String username,
        String password
) {

    public OpsAccessProperties {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("ops.username must be configured");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("ops.password must be configured");
        }
    }
}
