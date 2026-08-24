package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.infrastructure.PublicInstagramMetadataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentImageStorageServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAllImagesWithIndependentImageKeys() throws Exception {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(10L);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(10L)).thenReturn(List.of());
        when(downloader.download("https://scontent.cdninstagram.com/first.jpg"))
                .thenReturn(downloaded("first"));
        when(downloader.download("https://scontent.cdninstagram.com/second.jpg"))
                .thenReturn(downloaded("second"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.storeIfAvailable(content, List.of(
                "https://scontent.cdninstagram.com/first.jpg",
                "https://scontent.cdninstagram.com/second.jpg"
        ));

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<ContentImage> images = captor.getValue();
        assertThat(images).hasSize(2);
        assertThat(images).extracting(ContentImage::getImageKey)
                .doesNotHaveDuplicates();
        assertThat(images).extracting(ContentImage::getContentType)
                .containsOnly(MediaType.IMAGE_JPEG.toString());
        assertThat(Files.list(temporaryDirectory).count()).isEqualTo(2);
    }

    @Test
    void loadsImageByOpaqueImageKeyAndLazilyStoresMissingFile() throws Exception {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        ContentImage image = ContentImage.create(
                11L,
                "https://scontent.cdninstagram.com/old.jpg",
                "hash",
                0,
                NOW
        );
        when(imageRepository.findById(image.getImageKey())).thenReturn(Optional.of(image));
        when(contentRepository.findByIdAndPublicationStatus(11L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content(11L)));
        when(downloader.download("https://scontent.cdninstagram.com/old.jpg"))
                .thenReturn(downloaded("old"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        ContentImageStorageService.StoredImage result = service.load(image.getImageKey());

        assertThat(result.bytes()).containsExactly("old".getBytes());
        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
        assertThat(image.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
    }

    @Test
    void refreshesExpiredInstagramSourceWithOriginalContentImage() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(12L);
        ContentImage image = ContentImage.create(
                12L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                1,
                NOW
        );
        when(imageRepository.findById(image.getImageKey())).thenReturn(Optional.of(image));
        when(contentRepository.findByIdAndPublicationStatus(12L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content));
        when(downloader.download("https://scontent.cdninstagram.com/expired.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of(
                        "https://scontent.cdninstagram.com/fresh-first.jpg",
                        "https://scontent.cdninstagram.com/fresh-second.jpg"
                ));
        when(downloader.download("https://scontent.cdninstagram.com/fresh-second.jpg"))
                .thenReturn(downloaded("fresh"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        ContentImageStorageService.StoredImage result = service.load(image.getImageKey());

        assertThat(result.bytes()).containsExactly("fresh".getBytes());
        assertThat(image.getSourceUrl()).isEqualTo("https://scontent.cdninstagram.com/fresh-second.jpg");
        assertThat(image.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
    }

    @Test
    void refreshesExistingImageKeyWithoutCreatingAnotherImage() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(13L);
        ContentImage image = ContentImage.create(
                13L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                0,
                NOW
        );
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(13L)).thenReturn(List.of(image));
        when(downloader.download("https://scontent.cdninstagram.com/fresh.jpg"))
                .thenReturn(downloaded("fresh"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.refreshExistingIfAvailable(content, List.of("https://scontent.cdninstagram.com/fresh.jpg"));

        verify(imageRepository).saveAll(List.of(image));
        assertThat(image.getSourceUrl()).isEqualTo("https://scontent.cdninstagram.com/fresh.jpg");
        assertThat(image.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
    }

    private ContentImageStorageService service(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            ContentThumbnailDownloader downloader,
            PublicInstagramMetadataProvider metadataProvider
    ) {
        return new ContentImageStorageService(
                imageRepository,
                contentRepository,
                downloader,
                metadataProvider,
                new ContentThumbnailProperties(
                        "http://localhost:8080",
                        temporaryDirectory.toString(),
                        5,
                        5_000_000L,
                        10
                ),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private ContentThumbnailDownloader.DownloadedThumbnail downloaded(String value) {
        return new ContentThumbnailDownloader.DownloadedThumbnail(value.getBytes(), MediaType.IMAGE_JPEG);
    }

    private Content content(Long id) {
        Content content = Content.create(
                "https://www.instagram.com/reel/example-" + id,
                "hash-" + id,
                ContentSourceType.INSTAGRAM_REEL,
                "title",
                "caption",
                "https://scontent.cdninstagram.com/old.jpg",
                "content-hash",
                NOW
        );
        ReflectionTestUtils.setField(content, "id", id);
        content.publish(NOW);
        return content;
    }
}
