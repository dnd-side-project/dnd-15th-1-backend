package kr.omong.dulpick.domain.notification.infrastructure;

public class PushRegistrationEncryptionException extends RuntimeException {

    public PushRegistrationEncryptionException() {
        super("Push registration encryption failed");
    }

    public PushRegistrationEncryptionException(Throwable cause) {
        super("Push registration encryption failed", cause);
    }
}
