package kr.omong.dulpick.domain.testauth.security;

import kr.omong.dulpick.domain.testauth.config.TestAuthProperties;
import kr.omong.dulpick.domain.testauth.presentation.TestAuthController;
import kr.omong.dulpick.global.exception.SecurityExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@ConditionalOnProperty(
        prefix = "features.test-auth",
        name = "enabled",
        havingValue = "true"
)
public class TestAuthSecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            TestAuthController.BASE_PATH + "/signup",
            TestAuthController.BASE_PATH + "/login",
            TestAuthController.BASE_PATH + "/reissue"
    };

    @Bean
    @Order(-2)
    public SecurityFilterChain testAuthPublicSecurityFilterChain(
            HttpSecurity http,
            TestAuthProperties properties,
            SecurityExceptionHandler securityExceptionHandler
    ) throws Exception {
        return configureStateless(http)
                .securityMatcher(PUBLIC_PATHS)
                .addFilterBefore(
                        accessKeyFilter(properties, securityExceptionHandler),
                        AuthorizationFilter.class
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    @Order(-1)
    public SecurityFilterChain testAuthLogoutSecurityFilterChain(
            HttpSecurity http,
            TestAuthProperties properties,
            SecurityExceptionHandler securityExceptionHandler
    ) throws Exception {
        return configureStateless(http)
                .securityMatcher(TestAuthController.BASE_PATH + "/logout")
                .addFilterBefore(
                        accessKeyFilter(properties, securityExceptionHandler),
                        BearerTokenAuthenticationFilter.class
                )
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }

    private TestAuthAccessKeyFilter accessKeyFilter(
            TestAuthProperties properties,
            SecurityExceptionHandler securityExceptionHandler
    ) {
        return new TestAuthAccessKeyFilter(properties, securityExceptionHandler);
    }

    private HttpSecurity configureStateless(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
    }
}
