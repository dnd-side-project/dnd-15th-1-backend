package kr.omong.dulpick.domain.testauth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.omong.dulpick.domain.testauth.config.TestAuthProperties;
import kr.omong.dulpick.global.exception.SecurityExceptionHandler;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TestAuthAccessKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Test-Auth-Key";

    private final byte[] expectedAccessKey;
    private final SecurityExceptionHandler securityExceptionHandler;

    public TestAuthAccessKeyFilter(
            TestAuthProperties properties,
            SecurityExceptionHandler securityExceptionHandler
    ) {
        validateAccessKey(properties.accessKey());
        this.expectedAccessKey = properties.accessKey().getBytes(StandardCharsets.UTF_8);
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (hasValidAccessKey(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        securityExceptionHandler.commence(
                request,
                response,
                new BadCredentialsException("Test authentication access key is invalid")
        );
    }

    private boolean hasValidAccessKey(HttpServletRequest request) {
        String providedAccessKey = request.getHeader(HEADER_NAME);
        if (providedAccessKey == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedAccessKey,
                providedAccessKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validateAccessKey(String accessKey) {
        if (accessKey == null || accessKey.length() < 32) {
            throw new IllegalStateException(
                    "TEST_AUTH_ACCESS_KEY must contain at least 32 characters"
            );
        }
    }
}
