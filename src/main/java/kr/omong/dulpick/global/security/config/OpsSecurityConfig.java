package kr.omong.dulpick.global.security.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableConfigurationProperties(OpsAccessProperties.class)
public class OpsSecurityConfig {

    static final String[] OPS_PATHS = {
            "/ops",
            "/ops/**",
            "/api/v1/admin/**"
    };

    @Bean
    public CsrfTokenRepository opsCsrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    @Order(0)
    public SecurityFilterChain opsSecurityFilterChain(
            HttpSecurity http,
            OpsAccessProperties properties,
            ObjectProvider<PasswordEncoder> passwordEncoders,
            @Qualifier("opsCsrfTokenRepository") CsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        PasswordEncoder encoder = passwordEncoders.getIfAvailable(BCryptPasswordEncoder::new);
        UserDetailsService opsUsers = new InMemoryUserDetailsManager(
                User.builder()
                        .username(properties.username())
                        .password(encoder.encode(properties.password()))
                        .roles("OPS")
                        .build()
        );
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(opsUsers);
        authenticationProvider.setPasswordEncoder(encoder);
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler =
                new CsrfTokenRequestAttributeHandler();
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            String accept = request.getHeader("Accept");
            if (!request.getRequestURI().startsWith("/api/")
                    && accept != null
                    && accept.contains(MediaType.TEXT_HTML_VALUE)) {
                response.sendRedirect("/ops/login");
                return;
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        };
        return http
                .securityMatcher(OPS_PATHS)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authenticationProvider(authenticationProvider)
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .formLogin(form -> form
                        .loginPage("/ops/login")
                        .loginProcessingUrl("/ops/login")
                        .defaultSuccessUrl("/ops/places", true)
                        .failureUrl("/ops/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/ops/logout")
                        .logoutSuccessUrl("/ops/login")
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/ops/login").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .build();
    }
}
