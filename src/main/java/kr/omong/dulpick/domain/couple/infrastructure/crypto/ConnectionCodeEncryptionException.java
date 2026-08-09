package kr.omong.dulpick.domain.couple.infrastructure.crypto;

public class ConnectionCodeEncryptionException extends IllegalStateException {

    public ConnectionCodeEncryptionException() {
        super("Connection code encryption configuration is invalid");
    }

    public ConnectionCodeEncryptionException(Throwable cause) {
        super("Failed to process connection code encryption", cause);
    }
}
