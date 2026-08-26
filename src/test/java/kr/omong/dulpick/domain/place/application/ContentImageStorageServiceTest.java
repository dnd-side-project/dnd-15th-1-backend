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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void returnsUnavailableWithoutExternalCallWhenStoredFileIsMissing() {
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
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        verifyNoInteractions(downloader, metadataProvider);
    }

    @Test
    void refreshesMissingFileInBackgroundAndServesOnNextLoad() {
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
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(12L)).thenReturn(List.of(image));
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
                imageRepository, contentRepository, downloader, metadataProvider,
                Runnable::run
        );

        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        ContentImageStorageService.StoredImage result = service.load(image.getImageKey());

        assertThat(result.bytes()).containsExactly("fresh".getBytes());
        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_JPEG);
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

    @Test
    void refetchesInstagramPageWhenFreshCdnUrlIsRejected() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(14L);
        ContentImage image = ContentImage.create(
                14L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                0,
                NOW
        );
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(14L)).thenReturn(List.of(image));
        when(downloader.download("https://scontent.cdninstagram.com/initial-fresh.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of("https://scontent.cdninstagram.com/second-fresh.jpg"));
        when(downloader.download("https://scontent.cdninstagram.com/second-fresh.jpg"))
                .thenReturn(downloaded("fresh"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.refreshExistingIfAvailable(content, List.of(
                "https://scontent.cdninstagram.com/initial-fresh.jpg"
        ));

        assertThat(image.getSourceUrl()).isEqualTo("https://scontent.cdninstagram.com/second-fresh.jpg");
        assertThat(image.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
    }

    @Test
    void doesNotCreateDuplicateWhenFallbackUsesAnotherListedUrl() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(15L);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(15L)).thenReturn(List.of());
        when(downloader.download("https://scontent.cdninstagram.com/blocked.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        when(downloader.download("https://scontent.cdninstagram.com/available.jpg"))
                .thenReturn(downloaded("available"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.storeIfAvailable(content, List.of(
                "https://scontent.cdninstagram.com/blocked.jpg",
                "https://scontent.cdninstagram.com/available.jpg"
        ));

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<ContentImage> images = captor.getValue();
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getSourceUrl())
                .isEqualTo("https://scontent.cdninstagram.com/available.jpg");
    }

    @Test
    void refetchesOriginalInstagramPageWhenInitialImageUrlsAreExpired() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(16L);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(16L)).thenReturn(List.of());
        when(downloader.download("https://scontent.cdninstagram.com/expired.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of("https://scontent.cdninstagram.com/fresh.jpg"));
        when(downloader.download("https://scontent.cdninstagram.com/fresh.jpg"))
                .thenReturn(downloaded("fresh"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.storeIfAvailable(content, List.of(
                "https://scontent.cdninstagram.com/expired.jpg"
        ));

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<ContentImage> images = captor.getValue();
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getSourceUrl())
                .isEqualTo("https://scontent.cdninstagram.com/fresh.jpg");
    }

    @Test
    void persistsFailedImagesForLaterRetry() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(17L);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(17L)).thenReturn(List.of());
        when(downloader.download("https://scontent.cdninstagram.com/blocked.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl())).thenReturn(List.of());
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider
        );

        service.storeIfAvailable(content, List.of("https://scontent.cdninstagram.com/blocked.jpg"));

        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(imageRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<ContentImage> images = captor.getValue();
        assertThat(images).hasSize(1);
        assertThat(images.getFirst().getContentType()).isNull();
    }

    @Test
    void removesDuplicateImagesWhenCdnUrlsPointToTheSameBytes() throws Exception {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(18L);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(18L)).thenReturn(List.of());
        when(downloader.download("https://scontent.cdninstagram.com/first.jpg"))
                .thenReturn(downloaded("same"));
        when(downloader.download("https://scontent.cdninstagram.com/second.jpg"))
                .thenReturn(downloaded("same"));
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
        assertThat(images).hasSize(1);
        assertThat(Files.list(temporaryDirectory).count()).isEqualTo(1);
    }

    @Test
    void schedulesOriginalRefreshWithoutBlockingImageRequest() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(19L);
        ContentImage image = ContentImage.create(
                19L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                0,
                NOW
        );
        AtomicReference<Runnable> task = new AtomicReference<>();
        when(imageRepository.findById(image.getImageKey())).thenReturn(Optional.of(image));
        when(contentRepository.findByIdAndPublicationStatus(19L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content));
        when(downloader.download("https://scontent.cdninstagram.com/expired.jpg"))
                .thenThrow(new PublicContentImageUnavailableException());
        ContentImageStorageService service = service(
                imageRepository,
                contentRepository,
                downloader,
                metadataProvider,
                task::set
        );

        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        assertThat(task.get()).isNotNull();
        verifyNoInteractions(metadataProvider);
    }

    @Test
    void reportsContentImageAsMissingWhenDatabaseMetadataExistsButFileDoesNot() {
        ContentImage image = ContentImage.create(
                20L,
                "https://scontent.cdninstagram.com/missing.jpg",
                "missing-hash",
                0,
                NOW
        );
        image.markStored(MediaType.IMAGE_JPEG.toString(), NOW);
        ContentImageStorageService service = service(
                mock(ContentImageRepository.class),
                mock(ContentRepository.class),
                mock(ContentThumbnailDownloader.class),
                mock(PublicInstagramMetadataProvider.class)
        );

        assertThat(service.hasStoredFile(image)).isFalse();
    }

    @Test
    void reportsContentImageAsStoredOnlyWhenFileExists() throws Exception {
        ContentImage image = ContentImage.create(
                21L,
                "https://scontent.cdninstagram.com/stored.jpg",
                "stored-hash",
                0,
                NOW
        );
        image.markStored(MediaType.IMAGE_JPEG.toString(), NOW);
        Files.write(temporaryDirectory.resolve(image.getStorageKey()), "image".getBytes());
        ContentImageStorageService service = service(
                mock(ContentImageRepository.class),
                mock(ContentRepository.class),
                mock(ContentThumbnailDownloader.class),
                mock(PublicInstagramMetadataProvider.class)
        );

        assertThat(service.hasStoredFile(image)).isTrue();
    }

    @Test
    void refreshesAllBrokenImagesOfSameContentWithSingleMetadataFetch() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(22L);
        ContentImage first = ContentImage.create(
                22L,
                "https://scontent.cdninstagram.com/broken-a.jpg",
                "broken-hash-a",
                0,
                NOW
        );
        ContentImage second = ContentImage.create(
                22L,
                "https://scontent.cdninstagram.com/broken-b.jpg",
                "broken-hash-b",
                1,
                NOW
        );
        when(imageRepository.findById(first.getImageKey())).thenReturn(Optional.of(first));
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(22L))
                .thenReturn(List.of(first, second));
        when(contentRepository.findByIdAndPublicationStatus(22L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content));
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of(
                        "https://scontent.cdninstagram.com/fresh-a.jpg",
                        "https://scontent.cdninstagram.com/fresh-b.jpg"
                ));
        when(downloader.download("https://scontent.cdninstagram.com/fresh-a.jpg"))
                .thenReturn(downloaded("fresh-a"));
        when(downloader.download("https://scontent.cdninstagram.com/fresh-b.jpg"))
                .thenReturn(downloaded("fresh-b"));
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider, Runnable::run
        );

        assertThatThrownBy(() -> service.load(first.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        verify(metadataProvider, org.mockito.Mockito.times(1))
                .fetchImageUrls(content.getCanonicalUrl());
        assertThat(first.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
        assertThat(second.getContentType()).isEqualTo(MediaType.IMAGE_JPEG.toString());
    }

    @Test
    void coalescesConcurrentRefreshRequestsForTheSameContent() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(23L);
        ContentImage image = ContentImage.create(
                23L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                0,
                NOW
        );
        java.util.List<Runnable> submittedTasks = new java.util.ArrayList<>();
        when(imageRepository.findById(image.getImageKey())).thenReturn(Optional.of(image));
        when(contentRepository.findByIdAndPublicationStatus(23L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content));
        ContentImageStorageService service = service(
                imageRepository,
                contentRepository,
                downloader,
                metadataProvider,
                submittedTasks::add
        );

        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);
        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        assertThat(submittedTasks).hasSize(1);
        verifyNoInteractions(downloader, metadataProvider);
    }

    @Test
    void throttlesRepeatedBackgroundRefreshWithinMinimumInterval() {
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentThumbnailDownloader downloader = mock(ContentThumbnailDownloader.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        Content content = content(24L);
        ContentImage image = ContentImage.create(
                24L,
                "https://scontent.cdninstagram.com/expired.jpg",
                "expired-hash",
                0,
                NOW
        );
        when(imageRepository.findById(image.getImageKey())).thenReturn(Optional.of(image));
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(24L)).thenReturn(List.of(image));
        when(contentRepository.findByIdAndPublicationStatus(24L, ContentPublicationStatus.PUBLIC))
                .thenReturn(Optional.of(content));
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of("https://scontent.cdninstagram.com/fresh.jpg"));
        when(downloader.download(org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new PublicContentImageUnavailableException());
        ContentImageStorageService service = service(
                imageRepository, contentRepository, downloader, metadataProvider, Runnable::run
        );

        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);
        assertThatThrownBy(() -> service.load(image.getImageKey()))
                .isInstanceOf(PublicContentImageUnavailableException.class);

        verify(downloader, org.mockito.Mockito.times(1))
                .download(org.mockito.ArgumentMatchers.anyString());
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
                Clock.fixed(NOW, ZoneOffset.UTC),
                transactionManager()
        );
    }

    private ContentImageStorageService service(
            ContentImageRepository imageRepository,
            ContentRepository contentRepository,
            ContentThumbnailDownloader downloader,
            PublicInstagramMetadataProvider metadataProvider,
            Executor refreshExecutor
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
                Clock.fixed(NOW, ZoneOffset.UTC),
                transactionManager(),
                refreshExecutor,
                null,
                null
        );
    }

    private org.springframework.transaction.PlatformTransactionManager transactionManager() {
        org.springframework.transaction.PlatformTransactionManager manager =
                mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(manager.getTransaction(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        return manager;
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
