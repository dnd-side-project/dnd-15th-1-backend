package kr.omong.dulpick.domain.place.presentation.dto.response;

import kr.omong.dulpick.domain.place.application.PublicContentView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicContentResponseTest {

    @Test
    void replacesInstagramSourceThumbnailWithServerStorageUrl() {
        PublicContentView view = new PublicContentView(
                10L,
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                null,
                null,
                null,
                "title",
                "caption",
                "https://scontent.cdninstagram.com/image.jpg",
                List.of("image-key-1", "image-key-2"),
                0,
                List.of()
        );

        PublicContentResponse response = PublicContentResponse.from(view, "https://dulpick.omong.kr");

        assertThat(response.thumbnailUrl())
                .isEqualTo("https://dulpick.omong.kr/api/v1/content-images/image-key-1");
        assertThat(response.imageKeys()).containsExactly("image-key-1", "image-key-2");
    }

    @Test
    void keepsThumbnailMissingWhenSourceHasNoImage() {
        PublicContentView view = new PublicContentView(
                10L,
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                null,
                null,
                null,
                "title",
                "caption",
                null,
                List.of(),
                0,
                List.of()
        );

        assertThat(PublicContentResponse.from(view, "https://dulpick.omong.kr").thumbnailUrl())
                .isNull();
    }
}
