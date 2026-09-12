package kr.omong.dulpick.domain.analytics.application;

import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsEventService {

    private final AnalyticsEventRepository eventRepository;

    public AnalyticsEventService(AnalyticsEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void record(AnalyticsActionEvent action) {
        AnalyticsEvent.record(
                action.eventKey(),
                action.eventType(),
                action.memberId(),
                action.coupleId(),
                action.resourceType(),
                action.resourceId(),
                action.occurredAt()
        );
        eventRepository.insertIfAbsent(
                action.eventKey(),
                action.eventType().name(),
                action.memberId(),
                action.coupleId(),
                action.resourceType(),
                action.resourceId(),
                action.occurredAt(),
                action.occurredAt()
        );
    }
}
