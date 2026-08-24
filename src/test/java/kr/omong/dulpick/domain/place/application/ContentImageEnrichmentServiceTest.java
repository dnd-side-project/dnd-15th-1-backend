package kr.omong.dulpick.domain.place.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
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

    private void verifyNoStorageCall() {
        org.mockito.Mockito.verifyNoInteractions(storageService);
    }
}
