package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImageRepository;
import kr.omong.dulpick.domain.place.infrastructure.KakaoMapImageDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PlaceImageStorageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void reportsMissingPlaceImageWhenStorageKeyHasNoFile() {
        PlaceImageStorageService service = service();

        assertThat(service.isStored(UUID.randomUUID().toString())).isFalse();
    }

    @Test
    void reportsPlaceImageAsStoredWhenFileExistsWithinSizeLimit() throws Exception {
        String storageKey = UUID.randomUUID().toString();
        Files.write(temporaryDirectory.resolve("place-" + storageKey), "image".getBytes());
        PlaceImageStorageService service = service();

        assertThat(service.isStored(storageKey)).isTrue();
    }

    private PlaceImageStorageService service() {
        return new PlaceImageStorageService(
                mock(PlaceImageRepository.class),
                mock(KakaoMapImageDownloader.class),
                new ContentThumbnailProperties(
                        "http://localhost:8080",
                        temporaryDirectory.toString(),
                        5,
                        5_000_000L
                ),
                java.time.Clock.systemUTC()
        );
    }
}
