package kr.omong.dulpick.domain.place.application;

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlaceImageEnrichmentDispatcherTest {

    @Test
    void dispatchesImportImageEnrichmentWithoutBlockingCaller() {
        PlaceImageEnrichmentService service = mock(PlaceImageEnrichmentService.class);
        PlaceImageEnrichmentDispatcher dispatcher = new PlaceImageEnrichmentDispatcher(
                service,
                Runnable::run
        );

        dispatcher.dispatchImport(10L);

        verify(service).enrichImportPlaces(10L);
    }

    @Test
    void recordsRetryWhenImageWorkerQueueIsFull() {
        PlaceImageEnrichmentService service = mock(PlaceImageEnrichmentService.class);
        PlaceImageEnrichmentDispatcher dispatcher = new PlaceImageEnrichmentDispatcher(
                service,
                task -> {
                    throw new RejectedExecutionException();
                }
        );

        dispatcher.dispatchPlace(20L);

        verify(service).recordPlaceDispatchFailure(20L);
    }
}
