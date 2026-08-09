package kr.omong.dulpick.domain.notification.infrastructure;

public class PushSendException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;
    private final boolean invalidRegistration;

    public PushSendException(
            String errorCode,
            boolean retryable,
            boolean invalidRegistration,
            Throwable cause
    ) {
        super("Push delivery failed", cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.invalidRegistration = invalidRegistration;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public boolean isInvalidRegistration() {
        return invalidRegistration;
    }
}
