package kr.omong.dulpick.domain.auth.infrastructure.oidc;

import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SocialProviderProperties.class)
public class SocialOidcConfig {

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

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
        return new OidcSocialIdentityVerifier(provider, decoder, properties.audiences());
    }

    private JwtDecoder createDecoder(SocialProviderProperties.Provider properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .restOperations(new RestTemplate(requestFactory))
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new RequiredExpirationValidator(),
                new JwtTimestampValidator(CLOCK_SKEW),
                new IssuerAudienceValidator(properties.issuers(), properties.audiences())
        ));
        return decoder;
    }
}
