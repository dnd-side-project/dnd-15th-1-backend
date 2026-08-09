package kr.omong.dulpick.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification.marketing")
public record MarketingConsentProperties(String consentVersion) {

    public MarketingConsentProperties {
        if (consentVersion == null || consentVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "notification.marketing.consent-version must not be blank"
            );
        }
    }
}
