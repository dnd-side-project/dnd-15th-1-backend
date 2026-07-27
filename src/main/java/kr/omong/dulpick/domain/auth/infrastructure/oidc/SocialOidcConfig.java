package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableConfigurationProperties(SocialProviderProperties.class)
public class SocialOidcConfig {

    @Bean
    public SocialIdentityVerifier googleSocialIdentityVerifier(
            SocialProviderProperties properties
    ) {
        return createVerifier(SocialProvider.GOOGLE, properties.google());
    }

    @Bean
    public SocialIdentityVerifier kakaoSocialIdentityVerifier(
            SocialProviderProperties properties
    ) {
        return createVerifier(SocialProvider.KAKAO, properties.kakao());
    }

    @Bean
    public SocialIdentityVerifier appleSocialIdentityVerifier(
            SocialProviderProperties properties
    ) {
        return createVerifier(SocialProvider.APPLE, properties.apple());
    }

    private SocialIdentityVerifier createVerifier(
            SocialProvider provider,
            SocialProviderProperties.Provider properties
    ) {
        JwtDecoder decoder = createDecoder(properties);
        return new OidcSocialIdentityVerifier(provider, decoder);
    }

    private JwtDecoder createDecoder(SocialProviderProperties.Provider properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.jwkSetUri())
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new IssuerAudienceValidator(properties.issuers(), properties.audience())
        ));
        return decoder;
    }
}
