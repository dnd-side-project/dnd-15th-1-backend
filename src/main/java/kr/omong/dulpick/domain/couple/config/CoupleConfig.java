package kr.omong.dulpick.domain.couple.config;

import kr.omong.dulpick.domain.couple.infrastructure.crypto.ConnectionCodeCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties({
        CoupleProperties.class,
        ConnectionCodeEncryptionProperties.class
})
public class CoupleConfig {

    @Bean
    public ConnectionCodeCipher connectionCodeCipher(
            CoupleProperties coupleProperties,
            ConnectionCodeEncryptionProperties encryptionProperties,
            SecureRandom secureRandom
    ) {
        ConnectionCodeEncryptionProperties resolvedProperties = encryptionProperties
                .withFallbackKey(coupleProperties.codeEncryptionKey());
        return new ConnectionCodeCipher(resolvedProperties, secureRandom);
    }
}
