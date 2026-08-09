package kr.omong.dulpick.domain.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        FcmProperties.class,
        MarketingConsentProperties.class,
        NotificationMaintenanceProperties.class,
        PushProperties.class
})
public class NotificationPropertiesConfig {
}
