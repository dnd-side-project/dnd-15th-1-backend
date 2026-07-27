package kr.omong.dulpick.domain.auth.infrastructure.oidc;

public class InvalidSocialTokenException extends RuntimeException {

    public InvalidSocialTokenException(Throwable cause) {
        super("Social identity token is invalid", cause);
    }

    public InvalidSocialTokenException() {
        super("Social identity token is invalid");
    }
}
