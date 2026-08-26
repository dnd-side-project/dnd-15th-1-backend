package kr.omong.dulpick.domain.notification.application.admin;

public interface AnnouncementEmailSender {

    boolean smtpConfigured();

    void send(String toEmail, String nickname, String subject, String htmlBody);
}
