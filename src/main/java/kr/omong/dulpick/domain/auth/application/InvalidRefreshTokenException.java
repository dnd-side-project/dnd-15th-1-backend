package kr.omong.dulpick.domain.auth.application;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid or expired");
    }
}
