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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class PlaceImageWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-10T04:00:00Z");

    private final PlaceImageRepository imageRepository = mock(PlaceImageRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceImageStorageService storageService = mock(PlaceImageStorageService.class);
    private final PlaceImageWriter writer = new PlaceImageWriter(
            imageRepository,
            placeRepository,
            storageService,
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
        when(storageService.store(anyString()))
                .thenAnswer(invocation -> new PlaceImageStorageService.StoredImage(
                        "storage-" + invocation.getArgument(0).hashCode(),
                        org.springframework.http.MediaType.IMAGE_JPEG
                ));
        when(storageService.publicUrl(anyString()))
                .thenAnswer(invocation -> "https://dulpick.omong.kr/api/v1/place-images/"
                        + invocation.getArgument(0));

        writer.replace(20L, collectedUrls);

        verify(imageRepository).deleteAllByPlaceId(20L);
        verify(imageRepository).saveAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue())
                .extracting(PlaceImage::getImageUrl)
                .hasSize(5)
                .allMatch(url -> url.startsWith("https://dulpick.omong.kr/api/v1/place-images/"));
        verify(placeRepository).updateThumbnail(
                20L,
                "https://dulpick.omong.kr/api/v1/place-images/storage-" + collectedUrls.getFirst().hashCode(),
                NOW
        );
    }
}
