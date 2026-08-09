package kr.omong.dulpick.domain.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class FcmPushMessageProvider implements PushMessageProvider {

    private static final Set<MessagingErrorCode> RETRYABLE_CODES = EnumSet.of(
            MessagingErrorCode.INTERNAL,
            MessagingErrorCode.UNAVAILABLE,
            MessagingErrorCode.QUOTA_EXCEEDED
    );
    private static final Set<MessagingErrorCode> INVALID_REGISTRATION_CODES = EnumSet.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT
    );

    private final FirebaseMessaging firebaseMessaging;

    public FcmPushMessageProvider(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public String send(String registrationId, PushMessage message) {
        try {
            return firebaseMessaging.send(createMessage(registrationId, message));
        } catch (FirebaseMessagingException exception) {
            throw toPushSendException(exception);
        }
    }

    private Message createMessage(String registrationId, PushMessage pushMessage) {
        Message.Builder builder = Message.builder()
                .setToken(registrationId)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(pushMessage.title())
                        .setBody(pushMessage.body())
                        .build())
                .putData("notificationId", pushMessage.notificationId().toString())
                .putData("type", pushMessage.type().name())
                .putData("route", pushMessage.route().name());
        if (pushMessage.referenceId() != null) {
            builder.putData("referenceId", pushMessage.referenceId());
        }
        return builder.build();
    }

    private PushSendException toPushSendException(
            FirebaseMessagingException exception
    ) {
        MessagingErrorCode code = exception.getMessagingErrorCode();
        String errorCode = code == null
                ? String.valueOf(exception.getErrorCode())
                : code.name();
        return new PushSendException(
                errorCode,
                code != null && RETRYABLE_CODES.contains(code),
                code != null && INVALID_REGISTRATION_CODES.contains(code),
                exception
        );
    }
}
