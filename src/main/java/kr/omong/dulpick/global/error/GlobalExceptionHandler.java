package kr.omong.dulpick.global.error;

import kr.omong.dulpick.domain.auth.application.InvalidLoginNonceException;
import kr.omong.dulpick.domain.auth.application.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.InvalidSocialLoginRequestException;
import kr.omong.dulpick.domain.auth.infrastructure.apple.AppleAuthorizationException;
import kr.omong.dulpick.domain.auth.infrastructure.oidc.InvalidSocialTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation() {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request is invalid");
    }

    @ExceptionHandler(InvalidSocialLoginRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLoginRequest(
            InvalidSocialLoginRequestException exception
    ) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_SOCIAL_LOGIN", exception.getMessage());
    }

    @ExceptionHandler({
            InvalidLoginNonceException.class,
            InvalidRefreshTokenException.class,
            InvalidSocialTokenException.class,
            AppleAuthorizationException.class
    })
    public ResponseEntity<ErrorResponse> handleAuthentication() {
        return response(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed"
        );
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }
}
