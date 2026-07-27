package kr.omong.dulpick.domain.auth.application;

public class InvalidLoginNonceException extends RuntimeException {

    public InvalidLoginNonceException() {
        super("Login nonce is invalid, expired, or already used");
    }
}
