package kr.omong.dulpick.domain.notification.application.command;

import kr.omong.dulpick.domain.notification.application.exception.MarketingConsentVersionOutdatedException;
import kr.omong.dulpick.domain.notification.application.exception.MarketingConsentVersionRequiredException;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.config.MarketingConsentProperties;
import kr.omong.dulpick.domain.notification.domain.MarketingConsentHistory;
import kr.omong.dulpick.domain.notification.domain.MarketingConsentHistoryRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettings;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class NotificationSettingsService {

    private final MemberRepository memberRepository;
    private final MemberNotificationSettingsRepository settingsRepository;
    private final MarketingConsentHistoryRepository consentHistoryRepository;
    private final MarketingConsentProperties properties;
    private final Clock clock;

    public NotificationSettingsService(
            MemberRepository memberRepository,
            MemberNotificationSettingsRepository settingsRepository,
            MarketingConsentHistoryRepository consentHistoryRepository,
            MarketingConsentProperties properties,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.settingsRepository = settingsRepository;
        this.consentHistoryRepository = consentHistoryRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public NotificationSettingsView get(Long memberId) {
        Member member = lockActiveMember(memberId);
        MemberNotificationSettings settings = settingsRepository.findById(memberId)
                .orElseGet(() -> settingsRepository.save(
                        MemberNotificationSettings.create(member, clock.instant())
                ));
        return toView(settings);
    }

    @Transactional
    public NotificationSettingsView update(
            Long memberId,
            NotificationSettingsCommand command
    ) {
        Member member = lockActiveMember(memberId);
        MemberNotificationSettings settings = settingsRepository.findById(memberId)
                .orElseGet(() -> MemberNotificationSettings.create(member, clock.instant()));
        validateMarketingConsent(command);
        Instant now = clock.instant();
        boolean marketingChanged = settings.update(
                command.contentSavedEnabled(),
                command.dateScheduleEnabled(),
                command.marketingEnabled(),
                properties.consentVersion(),
                now
        );
        settingsRepository.save(settings);
        recordConsentChange(member, command.marketingEnabled(), marketingChanged, now);
        return toView(settings);
    }

    private Member lockActiveMember(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private void validateMarketingConsent(NotificationSettingsCommand command) {
        if (!command.marketingEnabled()) {
            return;
        }
        if (command.marketingConsentVersion() == null
                || command.marketingConsentVersion().isBlank()) {
            throw new MarketingConsentVersionRequiredException();
        }
        if (!properties.consentVersion().equals(command.marketingConsentVersion())) {
            throw new MarketingConsentVersionOutdatedException();
        }
    }

    private void recordConsentChange(
            Member member,
            boolean consented,
            boolean marketingChanged,
            Instant changedAt
    ) {
        if (!marketingChanged) {
            return;
        }
        consentHistoryRepository.save(MarketingConsentHistory.record(
                member,
                consented,
                properties.consentVersion(),
                changedAt
        ));
    }

    private NotificationSettingsView toView(MemberNotificationSettings settings) {
        return new NotificationSettingsView(
                settings.isContentSavedEnabled(),
                settings.isDateScheduleEnabled(),
                settings.isMarketingEnabled(),
                settings.getMarketingConsentVersion(),
                properties.consentVersion(),
                settings.getUpdatedAt()
        );
    }
}
