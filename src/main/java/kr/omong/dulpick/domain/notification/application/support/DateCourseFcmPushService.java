package kr.omong.dulpick.domain.notification.application.support;

import kr.omong.dulpick.domain.notification.application.event.DateCoursePlannedEvent;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettings;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import kr.omong.dulpick.domain.notification.domain.PushDevice;
import kr.omong.dulpick.domain.notification.domain.PushDeviceRepository;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import kr.omong.dulpick.domain.notification.infrastructure.PushMessage;
import kr.omong.dulpick.domain.notification.infrastructure.PushMessageProvider;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationCipher;
import kr.omong.dulpick.domain.notification.infrastructure.PushRegistrationEncryptionException;
import kr.omong.dulpick.domain.notification.infrastructure.PushSendException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DateCourseFcmPushService {

    private final MemberNotificationSettingsRepository settingsRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final PushRegistrationCipher registrationCipher;
    private final ObjectProvider<PushMessageProvider> pushMessageProvider;

    public DateCourseFcmPushService(
            MemberNotificationSettingsRepository settingsRepository,
            PushDeviceRepository pushDeviceRepository,
            PushRegistrationCipher registrationCipher,
            ObjectProvider<PushMessageProvider> pushMessageProvider
    ) {
        this.settingsRepository = settingsRepository;
        this.pushDeviceRepository = pushDeviceRepository;
        this.registrationCipher = registrationCipher;
        this.pushMessageProvider = pushMessageProvider;
    }

    @Transactional(readOnly = true)
    public void send(DateCoursePlannedEvent event) {
        PushMessageProvider provider = pushMessageProvider.getIfAvailable();
        if (provider == null || !isDatePushEnabled(event.partnerMemberId())) {
            return;
        }
        PushMessage message = new PushMessage(
                event.dateCourseId(),
                NotificationType.DATE_SCHEDULE_REMINDER,
                "%s님이 데이트코스를 짰어요!".formatted(event.plannerNickname()),
                "「%s」 일정을 확인해 주세요.".formatted(event.dateCourseTitle()),
                NotificationRoute.DATE_SCHEDULE,
                event.dateCourseId().toString()
        );
        pushDeviceRepository.findAllByMemberIdAndStatus(
                event.partnerMemberId(),
                PushDeviceStatus.ACTIVE
        ).forEach(device -> send(provider, device, message, event.partnerMemberId()));
    }

    private void send(
            PushMessageProvider provider,
            PushDevice device,
            PushMessage message,
            Long partnerMemberId
    ) {
        try {
            provider.send(registrationCipher.decrypt(device.getEncryptedRegistrationId()), message);
        } catch (PushSendException | PushRegistrationEncryptionException exception) {
            log.warn(
                    "Date course FCM send failed: partnerMemberId={}, deviceId={}",
                    partnerMemberId,
                    device.getId()
            );
        }
    }

    private boolean isDatePushEnabled(Long memberId) {
        return settingsRepository.findById(memberId)
                .map(MemberNotificationSettings::isDateScheduleEnabled)
                .orElse(true);
    }
}
