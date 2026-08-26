package kr.omong.dulpick.domain.notification.application.event;

import kr.omong.dulpick.domain.notification.application.command.NotificationCreationService;
import kr.omong.dulpick.domain.notification.application.command.NotificationRequest;
import kr.omong.dulpick.domain.notification.application.support.DateCourseFcmPushService;
import kr.omong.dulpick.domain.notification.domain.ContentSaveCounter;
import kr.omong.dulpick.domain.notification.domain.ContentSaveCounterRepository;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettings;
import kr.omong.dulpick.domain.notification.domain.MemberNotificationSettingsRepository;
import kr.omong.dulpick.domain.notification.domain.NotificationRoute;
import kr.omong.dulpick.domain.notification.domain.NotificationType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SelectiveNotificationEventListener {

    private final ContentSaveCounterRepository counterRepository;
    private final MemberNotificationSettingsRepository settingsRepository;
    private final NotificationCreationService notificationCreationService;
    private final DateCourseFcmPushService dateCourseFcmPushService;

    public SelectiveNotificationEventListener(
            ContentSaveCounterRepository counterRepository,
            MemberNotificationSettingsRepository settingsRepository,
            NotificationCreationService notificationCreationService,
            DateCourseFcmPushService dateCourseFcmPushService
    ) {
        this.counterRepository = counterRepository;
        this.settingsRepository = settingsRepository;
        this.notificationCreationService = notificationCreationService;
        this.dateCourseFcmPushService = dateCourseFcmPushService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onContentSaved(ContentSavedEvent event) {
        counterRepository.increase(
                event.coupleId(),
                event.saverMemberId(),
                event.occurredAt(),
                event.occurredAt()
        );
        ContentSaveCounter counter = counterRepository.findForUpdate(
                event.coupleId(),
                event.saverMemberId()
        ).orElseThrow(() -> new IllegalStateException(
                "Content save counter was not created"
        ));
        long saveCount = counter.getSaveCount();
        if (!counter.reachNewMilestone()) {
            return;
        }
        counter.markNotified();
        createContentMilestone(event, saveCount);
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onDateScheduleReminder(DateScheduleReminderDueEvent event) {
        event.receiverMemberIds().forEach(memberId -> createDateReminder(
                event,
                memberId
        ));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDateCoursePlanned(DateCoursePlannedEvent event) {
        dateCourseFcmPushService.send(event);
    }

    private void createContentMilestone(ContentSavedEvent event, long milestone) {
        NotificationRequest request = new NotificationRequest(
                event.partnerMemberId(),
                NotificationType.CONTENT_SAVE_MILESTONE,
                "새로운 저장 콘텐츠가 쌓였어요",
                "상대방이 콘텐츠를 %d개 저장했어요.".formatted(milestone),
                NotificationRoute.SAVED_CONTENTS,
                null,
                "CONTENT_MILESTONE:%d:%d:%d:%d".formatted(
                        event.coupleId(),
                        event.saverMemberId(),
                        milestone,
                        event.partnerMemberId()
                ),
                event.occurredAt()
        );
        notificationCreationService.createNotification(
                request,
                isContentPushEnabled(event.partnerMemberId())
        );
    }

    private void createDateReminder(
            DateScheduleReminderDueEvent event,
            Long receiverMemberId
    ) {
        NotificationRequest request = new NotificationRequest(
                receiverMemberId,
                NotificationType.DATE_SCHEDULE_REMINDER,
                "데이트 일정이 곧 시작돼요",
                "예정된 데이트 일정을 확인해 주세요.",
                NotificationRoute.DATE_SCHEDULE,
                event.scheduleId().toString(),
                "DATE_REMINDER:%d:%s:%d".formatted(
                        event.scheduleId(),
                        event.reminderAt(),
                        receiverMemberId
                ),
                event.occurredAt()
        );
        notificationCreationService.createNotification(
                request,
                isDatePushEnabled(receiverMemberId)
        );
    }

    private boolean isContentPushEnabled(Long memberId) {
        return settingsRepository.findById(memberId)
                .map(MemberNotificationSettings::isContentSavedEnabled)
                .orElse(true);
    }

    private boolean isDatePushEnabled(Long memberId) {
        return settingsRepository.findById(memberId)
                .map(MemberNotificationSettings::isDateScheduleEnabled)
                .orElse(true);
    }
}
