package kr.omong.dulpick.domain.notification.application.support;

import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.exception.FieldValidationException;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Objects;

@Component
public class NotificationCursorCodec {

    private static final int CURSOR_BYTES = Long.BYTES;

    public String encode(Long notificationId) {
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        byte[] payload = ByteBuffer.allocate(CURSOR_BYTES)
                .putLong(notificationId)
                .array();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    }

    public Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(cursor);
            if (payload.length != CURSOR_BYTES) {
                throw invalidCursor();
            }
            long notificationId = ByteBuffer.wrap(payload).getLong();
            if (notificationId <= 0) {
                throw invalidCursor();
            }
            return notificationId;
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    private FieldValidationException invalidCursor() {
        return new FieldValidationException(
                ErrorCode.INVALID_INPUT,
                "cursor",
                "invalid",
                "유효하지 않은 알림 커서입니다"
        );
    }
}
