package kr.omong.dulpick.domain.notification.application.command;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.notification.domain.Notification;
import kr.omong.dulpick.domain.notification.domain.NotificationDelivery;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettings;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.domain.notification.domain.PushDevice;
import kr.omong.dulpick.domain.notification.domain.PushDeviceRepository;
import kr.omong.dulpick.domain.notification.domain.PushDeviceStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationCreationService {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final MemberNotificationSettingsRepository settingsRepository;

    public NotificationCreationService(
            MemberRepository memberRepository,
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            PushDeviceRepository pushDeviceRepository,
            MemberNotificationSettingsRepository settingsRepository
    ) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.pushDeviceRepository = pushDeviceRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createSystemNotification(NotificationRequest request) {
        createNotification(request, true);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean createMarketingNotification(NotificationRequest request) {
        boolean enabled = settingsRepository.findById(request.receiverMemberId())
                .map(MemberNotificationSettings::isMarketingEnabled)
                .orElse(false);
        if (!enabled) {
            return false;
        }
        return createNotification(request, true);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean createNotification(NotificationRequest request, boolean pushEnabled) {
        Member receiver = memberRepository.findById(request.receiverMemberId()).orElse(null);
        if (receiver == null || !receiver.isActive()) {
            return false;
        }
        if (notificationRepository.existsByReceiverIdAndDeduplicationKey(
                request.receiverMemberId(),
                request.deduplicationKey()
        )) {
            return false;
        }
        Notification notification = notificationRepository.save(Notification.create(
                receiver,
                request.type(),
                request.title(),
                request.body(),
                request.route(),
                request.referenceId(),
                request.deduplicationKey(),
                request.occurredAt()
        ));
        if (pushEnabled) {
            createDeliveries(
                    request.receiverMemberId(),
                    notification,
                    request.occurredAt()
            );
        }
        return true;
    }

    private void createDeliveries(
            Long receiverMemberId,
            Notification notification,
            Instant createdAt
    ) {
        List<PushDevice> devices = pushDeviceRepository.findAllByMemberIdAndStatus(
                receiverMemberId,
                PushDeviceStatus.ACTIVE
        );
        deliveryRepository.saveAll(devices.stream()
                .map(device -> NotificationDelivery.pending(
                        notification,
                        device,
                        createdAt
                )).toList());
    }
}
