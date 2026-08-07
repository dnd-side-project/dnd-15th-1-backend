package kr.omong.dulpick.domain.notification.application.event;

import java.time.Instant;
import java.util.List;

public record DateScheduleReminderDueEvent(
        Long scheduleId,
        Instant reminderAt,
        List<Long> receiverMemberIds,
        Instant occurredAt
) {

    public DateScheduleReminderDueEvent {
        receiverMemberIds = List.copyOf(receiverMemberIds);
    }
}
