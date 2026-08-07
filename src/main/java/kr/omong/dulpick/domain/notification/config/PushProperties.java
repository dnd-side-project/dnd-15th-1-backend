package kr.omong.dulpick.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification.push")
public record PushProperties(String registrationEncryptionKey) {
}
