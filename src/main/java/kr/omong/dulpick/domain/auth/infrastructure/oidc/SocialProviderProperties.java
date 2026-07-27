package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

@ConfigurationProperties("auth.social.providers")
public record SocialProviderProperties(
        Provider google,
        Provider kakao,
        Provider apple
) {

    public record Provider(
            Set<String> issuers,
            String jwkSetUri,
            String audience
    ) {
    }
}
