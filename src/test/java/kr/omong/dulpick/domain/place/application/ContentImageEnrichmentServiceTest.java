package kr.omong.dulpick.domain.place.application;

import org.junit.jupiter.api.Test;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.RejectedExecutionException;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ContentImageEnrichmentServiceTest {

    private final ContentImageStorageService storageService = mock(ContentImageStorageService.class);
    private final ContentImageEnrichmentService service = new ContentImageEnrichmentService(
            storageService,
            Runnable::run
    );

    @Test
    void storesImagesThroughTheBackgroundTaskAfterDispatch() {
        service.dispatch(21L, List.of("https://cdninstagram.com/image.jpg"));

        verify(storageService).storeIfAvailable(
                21L,
                List.of("https://cdninstagram.com/image.jpg")
        );
    }

    @Test
    void skipsDispatchWithoutContentOrImageUrls() {
        service.dispatch(null, List.of("https://cdninstagram.com/image.jpg"));
        service.dispatch(21L, List.of());

        verifyNoStorageCall();
    }

    @Test
    void persistsRejectedImageTaskForRecoveryInsteadOfRunningOnCallerThread() throws Exception {
        ContentImageEnrichmentBacklogRepository backlogRepository =
                mock(ContentImageEnrichmentBacklogRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC);
        when(objectMapper.writeValueAsString(List.of("https://cdninstagram.com/image.jpg")))
                .thenReturn("[\"https://cdninstagram.com/image.jpg\"]");
        ContentImageEnrichmentService recoveryService = new ContentImageEnrichmentService(
                storageService,
                backlogRepository,
                objectMapper,
                clock,
                task -> {
                    throw new RejectedExecutionException();
                }
        );

        recoveryService.dispatch(21L, List.of("https://cdninstagram.com/image.jpg"));

        verify(storageService, org.mockito.Mockito.never())
                .storeIfAvailable(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList());
        verify(backlogRepository).enqueue(
                21L,
                "[\"https://cdninstagram.com/image.jpg\"]",
                Instant.parse("2026-08-24T12:00:00Z"),
                Instant.parse("2026-08-24T12:00:00Z")
        );
    }

    private void verifyNoStorageCall() {
        org.mockito.Mockito.verifyNoInteractions(storageService);
    }
}
