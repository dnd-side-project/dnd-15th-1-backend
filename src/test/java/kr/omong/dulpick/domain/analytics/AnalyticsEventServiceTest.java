package kr.omong.dulpick.domain.analytics;

import kr.omong.dulpick.domain.analytics.application.AnalyticsEventService;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsActionEvent;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventRepository;
import kr.omong.dulpick.domain.analytics.domain.AnalyticsEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalyticsEventServiceTest {

    private final AnalyticsEventRepository repository = mock(AnalyticsEventRepository.class);
    private final AnalyticsEventService service = new AnalyticsEventService(repository);

    @Test
    void insertsAnEventWithoutFailingOnAConcurrentDuplicate() {
        Instant occurredAt = Instant.parse("2026-09-10T00:00:00Z");
        AnalyticsActionEvent event = new AnalyticsActionEvent(
                "PLACE_SAVED:10",
                AnalyticsEventType.PLACE_SAVED,
                1L,
                2L,
                "PLACE",
                10L,
                occurredAt
        );

        service.record(event);

        verify(repository).insertIfAbsent(
                "PLACE_SAVED:10",
                "PLACE_SAVED",
                1L,
                2L,
                "PLACE",
                10L,
                occurredAt,
                occurredAt
        );
    }
}
