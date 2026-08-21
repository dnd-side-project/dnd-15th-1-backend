package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PlaceCategoryWriteThroughServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-21T00:00:00Z");

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceCategoryWriteThroughService service = new PlaceCategoryWriteThroughService(
            placeRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void fillsOnlyMissingStoredCategoryWithRecognizedKakaoCategory() {
        service.fillIfMissing(10L, null, null, "CE7", "음식점 > 카페");

        verify(placeRepository).updateCategoryIfMissing(
                10L,
                "CE7",
                "음식점 > 카페",
                NOW
        );
    }

    @Test
    void doesNotPersistUnknownKakaoCategoryFallback() {
        service.fillIfMissing(10L, null, null, null, "기타 > 미분류");

        verifyNoInteractions(placeRepository);
    }

    @Test
    void doesNotOverwriteExistingRecognizedCategory() {
        service.fillIfMissing(10L, "CE7", "음식점 > 카페", "FD6", "음식점");

        verifyNoInteractions(placeRepository);
    }
}
