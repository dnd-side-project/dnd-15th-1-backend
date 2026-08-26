package kr.omong.dulpick.domain.notification.infrastructure;

import kr.omong.dulpick.domain.notification.application.admin.AnnouncementEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "notification.email", name = "smtp-enabled",
        havingValue = "false", matchIfMissing = true)
public class LoggingAnnouncementEmailSender implements AnnouncementEmailSender {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAnnouncementEmailSender.class);

    @Override
    public boolean smtpConfigured() {
        return false;
    }

    @Override
    public void send(String toEmail, String nickname, String subject, String htmlBody) {
        logger.info(
                "announcement_email_logged email={}, nickname={}, subject={}",
                toEmail,
                nickname,
                subject
        );
    }
}
