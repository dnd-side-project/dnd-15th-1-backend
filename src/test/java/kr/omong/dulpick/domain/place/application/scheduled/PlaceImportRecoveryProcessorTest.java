package kr.omong.dulpick.domain.place.application.scheduled;

import kr.omong.dulpick.domain.place.application.PlaceImportDispatcher;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceImportRecoveryProcessorTest {

    @Test
    void resumesReceivedImportsAfterRecoveryDelay() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
        PlaceImportDispatcher dispatcher = mock(PlaceImportDispatcher.class);
        PlaceAnalysisProperties properties = new PlaceAnalysisProperties(
                true,
                100,
                10,
                1,
                false,
                600,
                300,
            3,
            Duration.ofSeconds(5),
            20,
            2,
            3
        );
        PlaceImportRecoveryProcessor processor = new PlaceImportRecoveryProcessor(
                importRepository,
                dispatcher,
                properties,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        when(importRepository.findRecoverableIds(
                now.minusSeconds(5),
                now.minusSeconds(600),
                PageRequest.of(0, 20)
        )).thenReturn(List.of(1L, 2L));
        processor.process();

        verify(dispatcher).dispatch(1L);
        verify(dispatcher).dispatch(2L);
    }
}
