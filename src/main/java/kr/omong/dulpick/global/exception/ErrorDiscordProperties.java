package kr.omong.dulpick.global.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties("monitoring.error-alert.discord")
public class ErrorDiscordProperties {

    private boolean enabled = false;
    private String webhookUrl = "";
    private Duration connectTimeout = Duration.ofSeconds(1);
    private Duration readTimeout = Duration.ofSeconds(1);
}
