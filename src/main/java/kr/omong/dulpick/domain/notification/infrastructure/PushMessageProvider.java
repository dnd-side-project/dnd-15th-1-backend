package kr.omong.dulpick.domain.notification.infrastructure;

public interface PushMessageProvider {

    String send(String registrationId, PushMessage message);
}
