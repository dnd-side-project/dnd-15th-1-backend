package kr.omong.dulpick.domain.notification.application;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.notification.domain.Notification;
import kr.omong.dulpick.domain.notification.domain.NotificationDelivery;
import kr.omong.dulpick.domain.notification.domain.NotificationDeliveryRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
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

    public NotificationCreationService(
            MemberRepository memberRepository,
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            PushDeviceRepository pushDeviceRepository
    ) {
        this.memberRepository = memberRepository;
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.pushDeviceRepository = pushDeviceRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void createSystemNotification(
            Long receiverMemberId,
            NotificationType type,
            String title,
            String body,
            NotificationRoute route,
            String referenceId,
            String deduplicationKey,
            Instant occurredAt
    ) {
        Member receiver = memberRepository.findById(receiverMemberId).orElse(null);
        if (receiver == null || !receiver.isActive()) {
            return;
        }
        if (notificationRepository.existsByReceiverIdAndDeduplicationKey(
                receiverMemberId,
                deduplicationKey
        )) {
            return;
        }
        Notification notification = notificationRepository.save(Notification.create(
                receiver,
                type,
                title,
                body,
                route,
                referenceId,
                deduplicationKey,
                occurredAt
        ));
        createDeliveries(receiverMemberId, notification, occurredAt);
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
