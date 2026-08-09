package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceImportTest {

    @Test
    void movesFromProcessingToReviewAndCompletedAfterConfirmation() {
        Instant createdAt = Instant.parse("2026-08-09T00:00:00Z");
        PlaceImport placeImport = PlaceImport.receive(
                1L,
                "https://www.instagram.com/reel/example",
                "hash",
                ContentSourceType.INSTAGRAM_REEL,
                createdAt
        );

        placeImport.start(createdAt.plusSeconds(1));
        placeImport.complete(
                "제목",
                "내용",
                null,
                "content-hash",
                createdAt,
                createdAt.plusSeconds(2)
        );
        assertThat(placeImport.getStatus()).isEqualTo(PlaceImportStatus.REVIEW_REQUIRED);

        placeImport.markCompleted(createdAt.plusSeconds(3));
        assertThat(placeImport.getStatus()).isEqualTo(PlaceImportStatus.COMPLETED);
    }

    @Test
    void failedImportCanBeRetried() {
        Instant createdAt = Instant.parse("2026-08-09T00:00:00Z");
        PlaceImport placeImport = PlaceImport.receive(
                1L,
                "https://www.instagram.com/p/example",
                "hash",
                ContentSourceType.INSTAGRAM_POST,
                createdAt
        );

        placeImport.fail("PLACE_ANALYSIS_UNAVAILABLE", createdAt.plusSeconds(1));
        placeImport.retry(createdAt.plusSeconds(2));

        assertThat(placeImport.getStatus()).isEqualTo(PlaceImportStatus.PROCESSING);
        assertThat(placeImport.getRetryCount()).isEqualTo(1);
        assertThat(placeImport.getFailureCode()).isNull();
    }
}
