package kr.omong.dulpick.global.exception;

public interface ErrorAlertSender {

    void sendCriticalAlert(String message);
}
