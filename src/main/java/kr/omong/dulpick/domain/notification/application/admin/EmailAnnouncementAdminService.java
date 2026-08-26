package kr.omong.dulpick.domain.notification.application.admin;

import kr.omong.dulpick.domain.notification.domain.EmailAnnouncement;
import kr.omong.dulpick.domain.notification.domain.EmailAnnouncementRepository;
import kr.omong.dulpick.domain.notification.domain.EmailOptOut;
import kr.omong.dulpick.domain.notification.domain.EmailOptOutRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EmailAnnouncementAdminService {

    public record AnnouncementRecipient(String email, String nickname) {
    }

    private static final String RECIPIENTS_SQL = """
            SELECT sa.email AS email, COALESCE(p.nickname, '') AS nickname
              FROM members m
              JOIN social_accounts sa
                ON sa.member_id = m.id AND sa.email IS NOT NULL AND sa.email <> ''
             LEFT JOIN member_profiles p ON p.member_id = m.id
             WHERE m.status = 'ACTIVE'
               AND sa.id = (SELECT MAX(latest.id) FROM social_accounts latest
                             WHERE latest.member_id = m.id)
               AND NOT EXISTS (SELECT 1 FROM email_opt_outs o
                                WHERE o.member_id = m.id AND o.category = ?)
             ORDER BY m.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final EmailAnnouncementRepository announcementRepository;
    private final EmailOptOutRepository optOutRepository;
    private final AnnouncementEmailSender emailSender;
    private final EmailTemplateRenderer templateRenderer;

    public EmailAnnouncementAdminService(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            EmailAnnouncementRepository announcementRepository,
            EmailOptOutRepository optOutRepository,
            AnnouncementEmailSender emailSender,
            EmailTemplateRenderer templateRenderer
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.announcementRepository = announcementRepository;
        this.optOutRepository = optOutRepository;
        this.emailSender = emailSender;
        this.templateRenderer = templateRenderer;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementRecipient> previewRecipients() {
        return jdbcTemplate.query(RECIPIENTS_SQL,
                (rs, rowNum) -> new AnnouncementRecipient(
                        rs.getString("email"),
                        rs.getString("nickname")
                ),
                EmailOptOut.CATEGORY_POLICY
        );
    }

    @Transactional
    public EmailAnnouncement send(String title, String body) {
        List<AnnouncementRecipient> recipients = previewRecipients();
        Instant now = clock.instant();
        EmailAnnouncement announcement = EmailAnnouncement.create(
                EmailOptOut.CATEGORY_POLICY, title, body, recipients.size(), now
        );
        announcementRepository.save(announcement);
        for (AnnouncementRecipient recipient : recipients) {
            emailSender.send(
                    recipient.email(),
                    recipient.nickname(),
                    title,
                    templateRenderer.renderPolicyNotice(recipient.nickname(), title, body)
            );
        }
        announcement.complete(emailSender.smtpConfigured()
                ? EmailAnnouncement.STATUS_COMPLETED_SMTP
                : EmailAnnouncement.STATUS_COMPLETED_LOG_ONLY);
        return announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<EmailAnnouncement> history(int page, int size) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<EmailOptOut> optOuts() {
        return optOutRepository.findAllByCategoryOrderByCreatedAtDesc(EmailOptOut.CATEGORY_POLICY);
    }

    @Transactional
    public boolean addOptOut(Long memberId) {
        if (optOutRepository.existsByMemberIdAndCategory(memberId, EmailOptOut.CATEGORY_POLICY)) {
            return false;
        }
        optOutRepository.save(EmailOptOut.create(
                memberId, EmailOptOut.CATEGORY_POLICY, clock.instant()
        ));
        return true;
    }

    @Transactional
    public void removeOptOut(Long memberId) {
        optOutRepository.findByMemberIdAndCategory(memberId, EmailOptOut.CATEGORY_POLICY)
                .ifPresent(optOutRepository::delete);
    }
}
