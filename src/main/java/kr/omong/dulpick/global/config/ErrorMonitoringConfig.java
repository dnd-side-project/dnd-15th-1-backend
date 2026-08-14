package kr.omong.dulpick.global.config;

import kr.omong.dulpick.global.exception.ErrorDiscordProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ErrorDiscordProperties.class)
public class ErrorMonitoringConfig {
}
