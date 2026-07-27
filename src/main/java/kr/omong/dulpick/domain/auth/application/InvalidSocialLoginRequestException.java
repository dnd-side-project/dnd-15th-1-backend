package kr.omong.dulpick.domain.auth.application;

public class InvalidSocialLoginRequestException extends RuntimeException {

    public InvalidSocialLoginRequestException(String message) {
        super(message);
    }
}
