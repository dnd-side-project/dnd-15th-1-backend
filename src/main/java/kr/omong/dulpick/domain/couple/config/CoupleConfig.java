package kr.omong.dulpick.domain.couple.config;

import kr.omong.dulpick.domain.couple.infrastructure.crypto.ConnectionCodeCipher;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;

@Configuration
@EnableConfigurationProperties(CoupleProperties.class)
public class CoupleConfig {

    @Bean
    public ConnectionCodeCipher connectionCodeCipher(
            CoupleProperties properties,
            SecureRandom secureRandom
    ) {
        return new ConnectionCodeCipher(properties.codeEncryptionKey(), secureRandom);
    }
}
