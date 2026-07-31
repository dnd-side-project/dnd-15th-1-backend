package kr.omong.dulpick.domain.testauth.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
        prefix = "features.test-auth",
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(TestAuthProperties.class)
public class TestAuthModuleConfig {

    @Bean
    public PasswordEncoder testAuthPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
