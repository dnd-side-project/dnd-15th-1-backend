package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ImageEnrichmentBacklogSqlIntegrationTest {

    @Autowired
    private ContentImageEnrichmentBacklogRepository contentBacklogRepository;

    @Autowired
    private PlaceImageEnrichmentBacklogRepository placeBacklogRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private Clock clock;

    @Test
    void validatesContentBacklogUpsertAndReactivation() {
        Instant now = clock.instant();
        Long contentId = contentRepository.save(Content.create(
                "https://www.instagram.com/reel/" + UUID.randomUUID(),
                UUID.randomUUID().toString(),
                ContentSourceType.INSTAGRAM_REEL,
                "title",
                "caption",
                null,
                UUID.randomUUID().toString(),
                now
        )).getId();

        contentBacklogRepository.enqueue(contentId, "[\"u\"]", now.plusSeconds(60), now);
        contentBacklogRepository.enqueue(contentId, "[\"u2\"]", now.plusSeconds(10), now);
        assertThat(contentBacklogRepository.existsByContentIdAndStatusIn(
                contentId, List.of("PENDING"))).isTrue();

        for (int i = 0; i < 6; i++) {
            contentBacklogRepository.scheduleRetry(
                    contentId, now.plusSeconds(60), now, 5);
        }
        assertThat(contentBacklogRepository.existsByContentIdAndStatusIn(
                contentId, List.of("PENDING"))).isFalse();

        contentBacklogRepository.enqueue(contentId, "[\"u3\"]", now.plusSeconds(5), now);
        assertThat(contentBacklogRepository.existsByContentIdAndStatusIn(
                contentId, List.of("PENDING", "PROCESSING"))).isTrue();
    }

    @Test
    void validatesPlaceBacklogClaimReleaseAndStaleReclaim() {
        Instant now = clock.instant();
        Long placeId = placeRepository.save(Place.create(
                "kakao-" + UUID.randomUUID(),
                "검증용 장소",
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        )).getId();

        placeBacklogRepository.recordFailure(placeId, "123456", "PROVIDER_ERROR", now);

        int firstClaim = placeBacklogRepository.claimRecovery(
                placeId, now.plusSeconds(1), now.minusSeconds(600), now);
        assertThat(firstClaim).isEqualTo(1);

        int duplicateClaimWhileProcessing = placeBacklogRepository.claimRecovery(
                placeId, now.plusSeconds(1), now.minusSeconds(600), now);
        assertThat(duplicateClaimWhileProcessing).isZero();

        int staleReclaimWhileFreshlyClaimed = placeBacklogRepository.claimRecovery(
                placeId, now.plusSeconds(1), now.plusSeconds(600), now);
        assertThat(staleReclaimWhileFreshlyClaimed).isEqualTo(1);

        placeBacklogRepository.releaseRecovery(placeId, now);
        int reclaimAfterRelease = placeBacklogRepository.claimRecovery(
                placeId, now.plusSeconds(1), now.minusSeconds(600), now);
        assertThat(reclaimAfterRelease).isEqualTo(1);

        placeBacklogRepository.releaseRecovery(placeId, now);
        int failed = placeBacklogRepository.markFailed(placeId, now);
        assertThat(failed).isEqualTo(1);

        int claimAfterFailed = placeBacklogRepository.claimRecovery(
                placeId, now.plusSeconds(1), now.minusSeconds(600), now);
        assertThat(claimAfterFailed).isZero();
    }
}
