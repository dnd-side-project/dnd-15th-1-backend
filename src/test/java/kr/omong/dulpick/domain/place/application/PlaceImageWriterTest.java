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
            transactionManager(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    private org.springframework.transaction.PlatformTransactionManager transactionManager() {
        org.springframework.transaction.PlatformTransactionManager manager =
                mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(manager.getTransaction(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        return manager;
    }

    @Test
    @SuppressWarnings("unchecked")
    void storesFirstPhotoAsThumbnailAndRemainingPhotosAsImages() {
        List<String> collectedUrls = List.of(
                "https://t1.kakaocdn.net/thumbnail",
                "https://t1.kakaocdn.net/image-1",
                "https://t1.kakaocdn.net/image-2",
                "https://t1.kakaocdn.net/image-3",
                "https://t1.kakaocdn.net/image-4",
                "https://t1.kakaocdn.net/image-5",
                "https://t1.kakaocdn.net/image-6",
                "https://t1.kakaocdn.net/image-7",
                "https://t1.kakaocdn.net/image-8",
                "https://t1.kakaocdn.net/image-9",
                "https://t1.kakaocdn.net/image-10",
                "https://t1.kakaocdn.net/image-11"
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
                .hasSize(10)
                .allMatch(url -> url.startsWith("https://dulpick.omong.kr/api/v1/place-images/"));
        verify(placeRepository).updateThumbnail(
                20L,
                "https://dulpick.omong.kr/api/v1/place-images/storage-" + collectedUrls.getFirst().hashCode(),
                NOW
        );
    }

    @Test
    void deletesPreviousStoredFilesOnlyAfterReplacingDatabaseRows() {
        PlaceImage previousImage = PlaceImage.createStored(
                20L,
                "https://dulpick.omong.kr/api/v1/place-images/old-storage",
                "old-hash",
                "old-storage",
                "image/jpeg",
                0,
                NOW
        );
        when(imageRepository.findAllByPlaceIdOrderByDisplayOrderAsc(20L))
                .thenReturn(List.of(previousImage));
        when(storageService.store(anyString()))
                .thenReturn(new PlaceImageStorageService.StoredImage(
                        "new-storage",
                        org.springframework.http.MediaType.IMAGE_JPEG
                ));
        when(storageService.publicUrl("new-storage"))
                .thenReturn("https://dulpick.omong.kr/api/v1/place-images/new-storage");

        writer.replace(20L, List.of("https://t1.kakaocdn.net/new-image"));

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                imageRepository, placeRepository, storageService
        );
        inOrder.verify(imageRepository).findAllByPlaceIdOrderByDisplayOrderAsc(20L);
        inOrder.verify(imageRepository).deleteAllByPlaceId(20L);
        inOrder.verify(imageRepository).saveAll(org.mockito.ArgumentMatchers.any());
        inOrder.verify(placeRepository).updateThumbnail(
                20L,
                "https://dulpick.omong.kr/api/v1/place-images/new-storage",
                NOW
        );
        inOrder.verify(storageService).delete("old-storage");
        verify(storageService, org.mockito.Mockito.never()).delete("new-storage");
    }

    @Test
    void skipsDuplicateDownloadedImagesByContentHash() {
        when(storageService.store(anyString()))
                .thenAnswer(invocation -> new PlaceImageStorageService.StoredImage(
                        "storage-" + invocation.getArgument(0).hashCode(),
                        org.springframework.http.MediaType.IMAGE_JPEG,
                        null,
                        "same-content"
                ));
        when(storageService.publicUrl(anyString()))
                .thenAnswer(invocation -> "https://dulpick.omong.kr/api/v1/place-images/"
                        + invocation.getArgument(0));

        writer.replace(20L, List.of(
                "https://t1.kakaocdn.net/image-1",
                "https://t1.kakaocdn.net/image-2"
        ));

        ArgumentCaptor<Iterable<PlaceImage>> imagesCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(imageRepository).saveAll(imagesCaptor.capture());
        assertThat(imagesCaptor.getValue()).hasSize(1);
        verify(storageService).delete("storage-" + "https://t1.kakaocdn.net/image-2".hashCode());
    }
}
