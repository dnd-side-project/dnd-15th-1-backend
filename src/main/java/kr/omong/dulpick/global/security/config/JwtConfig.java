package kr.omong.dulpick.global.security.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.global.security.validator.AccessTokenTypeValidator;
import kr.omong.dulpick.global.security.validator.MemberAccessTokenValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(
                new ImmutableSecret<SecurityContext>(properties.secretKey())
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
            JwtProperties properties,
            MemberRepository memberRepository
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(properties.secretKey())
                .build();
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new AccessTokenTypeValidator(),
                new MemberAccessTokenValidator(memberRepository)
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

}
