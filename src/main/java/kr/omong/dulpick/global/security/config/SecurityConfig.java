package kr.omong.dulpick.global.security.config;

import kr.omong.dulpick.global.exception.SecurityExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_PATHS = {
            "/api/v1/auth/nonce",
            "/api/v1/auth/social-login",
            "/api/v1/auth/reissue"
    };

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/content-images/*",
            "/api/v1/place-images/*",
            "/",
            "/index.html",
            "/privacy",
            "/privacy.html",
            "/terms",
            "/terms.html",
            "/marketing",
            "/marketing.html",
            "/connect",
            "/.well-known/apple-app-site-association",
            "/assets/**",
            "/favicon.ico",
            "/favicon.svg",
            "/favicon.png",
            "/health",
            "/actuator/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain publicAuthSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return configureStateless(http)
                .securityMatcher(PUBLIC_AUTH_PATHS)
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    public SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            SecurityExceptionHandler securityExceptionHandler
    ) throws Exception {
        return configureStateless(http)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATHS).permitAll()
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

    private HttpSecurity configureStateless(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
    }
}
