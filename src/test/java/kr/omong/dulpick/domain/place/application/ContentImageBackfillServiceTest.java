package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.ContentImageBackfillProperties;
import kr.omong.dulpick.domain.place.domain.Content;
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
    void refetchesInstagramMetadataAndRefreshesExistingImageKeys() {
        ContentRepository contentRepository = mock(ContentRepository.class);
        PublicInstagramMetadataProvider metadataProvider = mock(PublicInstagramMetadataProvider.class);
        ContentImageStorageService imageStorageService = mock(ContentImageStorageService.class);
        Content content = content(10L);
        when(contentRepository.findAllBySourceTypeInOrderByIdAsc(any())).thenReturn(List.of(content));
        when(metadataProvider.fetchImageUrls(content.getCanonicalUrl()))
                .thenReturn(List.of(
                        "https://scontent.cdninstagram.com/first.jpg",
                        "https://scontent.cdninstagram.com/second.jpg"
                ));
        when(imageStorageService.hasAllStoredImages(10L)).thenReturn(true);

        ContentImageBackfillService service = new ContentImageBackfillService(
                contentRepository,
                metadataProvider,
                imageStorageService,
                new ContentImageBackfillProperties(true, 10, 0)
        );

        ContentImageBackfillService.Result result = service.backfill();

        assertThat(result).isEqualTo(new ContentImageBackfillService.Result(1, 1, 0));
        verify(imageStorageService).refreshExistingIfAvailable(eq(content), eq(List.of(
                "https://scontent.cdninstagram.com/first.jpg",
                "https://scontent.cdninstagram.com/second.jpg"
        )));
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
