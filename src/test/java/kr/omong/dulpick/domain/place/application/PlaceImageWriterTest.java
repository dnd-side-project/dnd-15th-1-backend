package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceImage;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PlaceImageWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-10T04:00:00Z");

    private final PlaceImageRepository imageRepository = mock(PlaceImageRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceImageWriter writer = new PlaceImageWriter(
            imageRepository,
            placeRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    @SuppressWarnings("unchecked")
    void storesFirstPhotoAsThumbnailAndRemainingPhotosAsImages() {
        List<String> collectedUrls = List.of(
                "https://t1.kakaocdn.net/thumbnail",
                "https://t1.kakaocdn.net/image-1",
                "https://t1.kakaocdn.net/image-2",
                "https://t1.kakaocdn.net/image-3",
                "https://t1.kakaocdn.net/image-4"
        );
        ArgumentCaptor<Iterable<PlaceImage>> imagesCaptor = ArgumentCaptor.forClass(Iterable.class);

        writer.replace(20L, collectedUrls);

        verify(imageRepository).deleteAllByPlaceId(20L);
        verify(imageRepository).saveAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue())
                .extracting(PlaceImage::getImageUrl)
                .containsExactlyElementsOf(collectedUrls.subList(1, 5))
                .doesNotContain(collectedUrls.getFirst());
        verify(placeRepository).updateThumbnail(20L, collectedUrls.getFirst(), NOW);
    }
}
