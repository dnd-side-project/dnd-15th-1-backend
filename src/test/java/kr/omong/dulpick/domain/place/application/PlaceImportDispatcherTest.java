package kr.omong.dulpick.domain.place.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceImportDispatcherTest {

    @Test
    void claimsAndDispatchesImportImmediately() {
        PlaceImportProcessingService processingService = mock(PlaceImportProcessingService.class);
        Executor executor = Runnable::run;
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        PlaceImportDispatcher dispatcher = new PlaceImportDispatcher(processingService, executor, clock);
        when(processingService.claimPending(10L)).thenReturn("claim-token");

        dispatcher.dispatch(10L);

        verify(processingService).processClaimed(
                10L,
                "claim-token",
                Instant.parse("2026-08-24T12:00:00Z")
        );
    }
}
