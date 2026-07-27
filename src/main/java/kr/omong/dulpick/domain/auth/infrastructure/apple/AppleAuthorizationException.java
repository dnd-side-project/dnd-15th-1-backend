package kr.omong.dulpick.domain.auth.infrastructure.apple;

public class AppleAuthorizationException extends RuntimeException {

    public AppleAuthorizationException(String message) {
        super(message);
    }

    public AppleAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
