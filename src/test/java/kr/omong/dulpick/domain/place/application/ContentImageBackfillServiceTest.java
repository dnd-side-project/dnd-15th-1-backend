package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.ContentImageBackfillProperties;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImage;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.infrastructure.PublicInstagramMetadataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContentImageBackfillServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T00:00:00Z");

    @Test
    void refetchesInstagramMetadataAndStoresAllDiscoveredImages() {
        ContentRepository contentRepository = mock(ContentRepository.class);
        ContentImageRepository imageRepository = mock(ContentImageRepository.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        ContentImageStorageService imageStorageService = mock(ContentImageStorageService.class);
        Content content = content(10L);
        when(contentRepository.findAllBySourceTypeInOrderByIdAsc(any())).thenReturn(List.of(content));
        when(metadataProvider.fetch(content.getCanonicalUrl(), content.getSourceType()))
                .thenReturn(metadata());
        ContentImage storedImage = ContentImage.create(
                        10L,
                        "https://scontent.cdninstagram.com/first.jpg",
                        "hash",
                        0,
                        NOW
                );
        storedImage.markStored("image/jpeg", NOW);
        when(imageRepository.findAllByContentIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(storedImage));

        ContentImageBackfillService service = new ContentImageBackfillService(
                contentRepository,
                imageRepository,
                metadataProvider,
                imageStorageService,
                new ContentImageBackfillProperties(true, 10, 0)
        );

        ContentImageBackfillService.Result result = service.backfill();

        assertThat(result).isEqualTo(new ContentImageBackfillService.Result(1, 1, 0));
        verify(imageStorageService).storeIfAvailable(eq(content), eq(List.of(
                "https://scontent.cdninstagram.com/old.jpg",
                "https://scontent.cdninstagram.com/first.jpg",
                "https://scontent.cdninstagram.com/second.jpg"
        )));
    }

    private ContentMetadata metadata() {
        return new ContentMetadata(
                "https://www.instagram.com/reel/example-10",
                ContentSourceType.INSTAGRAM_REEL,
                "title",
                "caption",
                "https://scontent.cdninstagram.com/first.jpg",
                "content-hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                NOW,
                List.of(
                        "https://scontent.cdninstagram.com/first.jpg",
                        "https://scontent.cdninstagram.com/second.jpg"
                )
        );
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
        return content;
    }
}
