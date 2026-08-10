package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSubmissionRepository;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceImportResultWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final PlaceImportResultWriter writer = new PlaceImportResultWriter(
            mock(PlaceImportRepository.class),
            mock(PlaceCandidateRepository.class),
            mock(PlaceRepository.class),
            contentRepository,
            mock(ContentPlaceRepository.class),
            mock(ContentSubmissionRepository.class),
            Clock.fixed(NOW, ZoneOffset.UTC),
            objectMapper
    );

    @Test
    void completesAnalysisOnlyWhenClaimTokenStillOwnsTheRow() throws Exception {
        List<ExtractedPlace> candidates = List.of(new ExtractedPlace(
                "밀빛 망원점",
                "서울 마포구",
                "망원 카페 - 밀빛",
                "EXPLICIT_VENUE"
        ));
        when(objectMapper.writeValueAsString(candidates)).thenReturn("[{\"name\":\"밀빛 망원점\"}]");
        when(contentRepository.completeAnalysis(
                10L,
                "claim-token",
                "content-hash",
                "gemini-2.5-flash",
                "place-extraction-v3",
                "[{\"name\":\"밀빛 망원점\"}]",
                NOW
        )).thenReturn(0);

        boolean saved = writer.saveAnalysis(
                10L,
                "claim-token",
                "content-hash",
                "gemini-2.5-flash",
                "place-extraction-v3",
                candidates,
                NOW
        );

        assertThat(saved).isFalse();
        verify(contentRepository).completeAnalysis(
                10L,
                "claim-token",
                "content-hash",
                "gemini-2.5-flash",
                "place-extraction-v3",
                "[{\"name\":\"밀빛 망원점\"}]",
                NOW
        );
    }

    @Test
    void failsAnalysisWithTheSameClaimTokenCondition() {
        writer.failAnalysis(10L, "claim-token");

        verify(contentRepository).failAnalysis(10L, "claim-token");
    }
}
