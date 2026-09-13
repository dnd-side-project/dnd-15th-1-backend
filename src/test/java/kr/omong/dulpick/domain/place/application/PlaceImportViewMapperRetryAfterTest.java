package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceImportViewMapperRetryAfterTest {

    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");
    private final PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);

    private final PlaceImportViewMapper mapper = new PlaceImportViewMapper(
            candidateRepository,
            placeRepository,
            memberPlaceRepository,
            new PlaceAnalysisProperties(
                    true, 100, 10, 1, true, 600, 300, 3,
                    Duration.ofSeconds(5), 20, 8, 12
            )
    );

    @Test
    void asksClientToPollAfterInitialTwoSeconds() {
        when(candidateRepository.findAllByImportIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        PlaceImport placeImport = PlaceImport.receive(
                1L,
                "https://www.instagram.com/reel/example",
                "url-hash",
                ContentSourceType.INSTAGRAM_REEL,
                Instant.parse("2026-08-26T00:00:00Z")
        );
        placeImport.start(placeImport.getCreatedAt());

        assertThat(placeImport.getStatus()).isEqualTo(PlaceImportStatus.PROCESSING);
        assertThat(mapper.toView(placeImport).retryAfterSeconds())
                .isEqualTo(PlaceImportViewMapper.PROCESSING_RETRY_AFTER_SECONDS)
                .isEqualTo(2L);
    }

    @Test
    void keepsTwoSecondPollingHintWhileProcessing() {
        when(candidateRepository.findAllByImportIdOrderByIdAsc(anyLong())).thenReturn(List.of());
        for (long ageSeconds : List.of(0L, 2L, 6L, 10L, 15L)) {
            PlaceImport placeImport = PlaceImport.receive(
                    1L,
                    "https://www.instagram.com/reel/example-" + ageSeconds,
                    "url-hash-" + ageSeconds,
                    ContentSourceType.INSTAGRAM_REEL,
                    NOW.minusSeconds(ageSeconds)
            );
            placeImport.start(placeImport.getCreatedAt());
            assertThat(mapper.toView(placeImport).retryAfterSeconds()).isEqualTo(2L);
        }
    }
}
