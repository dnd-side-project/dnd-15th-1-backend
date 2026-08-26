package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentImageIntegrityServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    private final ContentImageRepository imageRepository = mock(ContentImageRepository.class);
    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ContentImageStorageService storageService = mock(ContentImageStorageService.class);
    private final ContentImageEnrichmentBacklogRepository backlogRepository =
            mock(ContentImageEnrichmentBacklogRepository.class);
    private final ContentImageIntegrityService service = new ContentImageIntegrityService(
            imageRepository,
            contentRepository,
            storageService,
            backlogRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void registersBacklogAndRepointsThumbnailToStoredSiblingWhenFileIsMissing() {
        ContentImage broken = image(30L, 0);
        ContentImage sibling = image(30L, 1);
        Content content = content(30L, "http://localhost:8080/api/v1/content-images/" + broken.getImageKey());
        when(imageRepository.findByContentTypeIsNotNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(broken)));
        when(storageService.hasStoredFile(broken)).thenReturn(false);
        when(backlogRepository.existsByContentIdAndStatusIn(30L, List.of("PENDING", "PROCESSING")))
                .thenReturn(false);
        when(contentRepository.findById(30L)).thenReturn(Optional.of(content));
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(30L))
                .thenReturn(List.of(broken, sibling));
        when(storageService.hasStoredFile(sibling)).thenReturn(true);
        when(storageService.publicUrl(broken.getImageKey()))
                .thenReturn("http://localhost:8080/api/v1/content-images/" + broken.getImageKey());
        when(storageService.publicUrl(sibling.getImageKey()))
                .thenReturn("http://localhost:8080/api/v1/content-images/" + sibling.getImageKey());

        service.auditStoredFiles();

        verify(storageService).registerMissingImageBacklog(30L);
        verify(contentRepository).save(content);
        org.junit.jupiter.api.Assertions.assertEquals(
                "http://localhost:8080/api/v1/content-images/" + sibling.getImageKey(),
                content.getThumbnailUrl()
        );
    }

    @Test
    void skipsHealthyImagesWithoutBacklogOrThumbnailChanges() {
        ContentImage healthy = image(31L, 0);
        when(imageRepository.findByContentTypeIsNotNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(healthy)));
        when(storageService.hasStoredFile(healthy)).thenReturn(true);

        service.auditStoredFiles();

        verifyNoInteractionsWithCollaborators();
    }

    @Test
    void doesNotDuplicateBacklogWhenActiveTaskAlreadyExists() {
        ContentImage broken = image(32L, 0);
        Content content = content(32L, null);
        when(imageRepository.findByContentTypeIsNotNull(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(broken)));
        when(storageService.hasStoredFile(broken)).thenReturn(false);
        when(backlogRepository.existsByContentIdAndStatusIn(32L, List.of("PENDING", "PROCESSING")))
                .thenReturn(true);
        when(contentRepository.findById(32L)).thenReturn(Optional.of(content));

        service.auditStoredFiles();

        verify(storageService, never()).registerMissingImageBacklog(32L);
        verify(contentRepository, never()).save(any());
    }

    private void verifyNoInteractionsWithCollaborators() {
        org.mockito.Mockito.verifyNoInteractions(backlogRepository, contentRepository);
    }

    private ContentImage image(Long contentId, int displayOrder) {
        return ContentImage.create(
                contentId,
                "https://scontent.cdninstagram.com/image-" + contentId + "-" + displayOrder + ".jpg",
                "hash-" + contentId + "-" + displayOrder,
                displayOrder,
                NOW
        );
    }

    private Content content(Long id, String thumbnailUrl) {
        Content content = Content.create(
                "https://www.instagram.com/reel/example-" + id,
                "hash-" + id,
                kr.omong.dulpick.domain.place.domain.ContentSourceType.INSTAGRAM_REEL,
                "title",
                "caption",
                thumbnailUrl,
                "content-hash",
                NOW
        );
        org.springframework.test.util.ReflectionTestUtils.setField(content, "id", id);
        return content;
    }
}
