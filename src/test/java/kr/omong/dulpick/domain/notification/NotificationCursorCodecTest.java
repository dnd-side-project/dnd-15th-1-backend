package kr.omong.dulpick.domain.notification;

import kr.omong.dulpick.domain.notification.application.NotificationCursorCodec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class NotificationCursorCodecTest {

    private final NotificationCursorCodec codec = new NotificationCursorCodec();

    @Test
    void roundTripsNotificationId() {
        assertThat(codec.decode(codec.encode(123L))).isEqualTo(123L);
    }

    @Test
    void rejectsNullNotificationIdAtEncodingBoundary() {
        assertThatNullPointerException()
                .isThrownBy(() -> codec.encode(null))
                .withMessage("notificationId must not be null");
    }
}
