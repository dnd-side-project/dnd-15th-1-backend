package kr.omong.dulpick.domain.auth.application.exception;

public class ConcurrentSocialAccountCreationException extends RuntimeException {

    public ConcurrentSocialAccountCreationException(Throwable cause) {
        super(cause);
    }
}
