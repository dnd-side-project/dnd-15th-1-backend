package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSubmissionRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceImportResultWriterTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");

    private final PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
    private final PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ContentPlaceRepository contentPlaceRepository = mock(ContentPlaceRepository.class);
    private final ContentSubmissionRepository submissionRepository = mock(ContentSubmissionRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final PlaceImportAnalysisWriter analysisWriter =
            new PlaceImportAnalysisWriter(contentRepository, objectMapper);
    private final PlaceImportContentWriter contentWriter = new PlaceImportContentWriter(
            importRepository,
            candidateRepository,
            placeRepository,
            contentRepository,
            contentPlaceRepository,
            submissionRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final PlaceImportResultWriter writer = new PlaceImportResultWriter(
            importRepository,
            candidateRepository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            analysisWriter,
            contentWriter
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

    @Test
    void storesOneCandidateForSameNormalizedKakaoPlace() {
        PlaceImport placeImport = mock(PlaceImport.class);
        Place place = mock(Place.class);
        when(importRepository.findClaimedForUpdate(1L, "import-claim"))
                .thenReturn(Optional.of(placeImport));
        when(placeImport.getContentId()).thenReturn(null);
        when(placeRepository.findByKakaoPlaceId("kakao-1")).thenReturn(Optional.of(place));
        when(place.getId()).thenReturn(20L);
        ContentMetadata metadata = new ContentMetadata(
                "https://map.naver.com/p/entry/place/1",
                ContentSourceType.NAVER_MAP,
                "을지식당",
                "서울 중구 을지로40길 17",
                null,
                "hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null
        );
        VerifiedPlace verifiedPlace = new VerifiedPlace(
                "kakao-1",
                "을지식당",
                "서울 중구 을지로6가 67-3",
                "서울 중구 을지로40길 17",
                null,
                null,
                "FD6",
                "음식점 > 한식",
                null
        );
        VerifiedCandidate reviewCandidate = candidate(
                "첫 번째 근거",
                verifiedPlace,
                PlaceVerificationStatus.REVIEW_REQUIRED
        );
        VerifiedCandidate verifiedCandidate = candidate(
                "두 번째 근거",
                verifiedPlace,
                PlaceVerificationStatus.VERIFIED
        );

        writer.saveSuccess(
                1L,
                "import-claim",
                metadata,
                List.of(reviewCandidate, verifiedCandidate)
        );

        verify(placeRepository, times(1)).insertIfAbsent(
                eq("kakao-1"),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verify(candidateRepository).saveAll(argThat(candidates -> {
            List<PlaceCandidate> saved = (List<PlaceCandidate>) candidates;
            return saved.size() == 1
                    && saved.getFirst().getVerificationStatus() == PlaceVerificationStatus.VERIFIED;
        }));
    }

    @Test
    void rejectsCandidateWritesAfterImportClaimIsLost() {
        ContentMetadata metadata = new ContentMetadata(
                "https://map.naver.com/p/entry/place/1",
                ContentSourceType.NAVER_MAP,
                "을지식당",
                "서울 중구 을지로40길 17",
                null,
                "hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> writer.saveSuccess(
                1L,
                "stale-claim",
                metadata,
                List.of()
        )).isInstanceOf(PlaceImportClaimLostException.class);

        verifyNoInteractions(candidateRepository, placeRepository);
    }

    private VerifiedCandidate candidate(
            String evidence,
            VerifiedPlace place,
            PlaceVerificationStatus status
    ) {
        return new VerifiedCandidate(
                new ExtractedPlace("을지식당", "서울 중구", evidence, "EXPLICIT_VENUE"),
                place,
                status
        );
    }
}
