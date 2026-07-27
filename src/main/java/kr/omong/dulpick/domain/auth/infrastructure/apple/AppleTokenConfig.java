package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AppleTokenProperties.class)
public class AppleTokenConfig {

    @Bean
    public AppleClientSecretGenerator appleClientSecretGenerator(
            AppleTokenProperties properties,
            Clock clock
    ) {
        return new AppleClientSecretGenerator(properties, clock);
    }

    @Bean
    public AppleTokenClient appleTokenClient(
            AppleTokenProperties properties,
            AppleClientSecretGenerator clientSecretGenerator
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        return new AppleTokenHttpClient(properties, clientSecretGenerator, restClient);
    }

    @Bean
    public ProviderTokenCipher providerTokenCipher(
            AppleTokenProperties properties,
            SecureRandom secureRandom
    ) {
        return new ProviderTokenCipher(properties.tokenEncryptionKey(), secureRandom);
    }
}
