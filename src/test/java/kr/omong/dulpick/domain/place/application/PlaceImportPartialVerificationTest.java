package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceImportPartialVerificationTest {

    private static final String CLAIM_TOKEN = "claim-token-1";
    private static final Instant NOW = Instant.parse("2026-08-26T00:00:00Z");

    private final PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
    private final PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceImportResultWriter resultWriter = mock(PlaceImportResultWriter.class);
    private final PlaceImageEnrichmentService imageEnrichmentService = mock(PlaceImageEnrichmentService.class);
    private final ContentImageEnrichmentService contentImageEnrichmentService =
            mock(ContentImageEnrichmentService.class);
    private final PlaceImportReservationService reservationService = mock(PlaceImportReservationService.class);
    private final MetadataService metadataService = mock(MetadataService.class);
    private final PlaceAnalyzer placeAnalyzer = mock(PlaceAnalyzer.class);
    private final PlaceVerifier placeVerifier = mock(PlaceVerifier.class);

    private final PlaceImportProcessingService service = new PlaceImportProcessingService(
            importRepository,
            candidateRepository,
            placeRepository,
            resultWriter,
            imageEnrichmentService,
            contentImageEnrichmentService,
            reservationService,
            metadataService,
            placeAnalyzer,
            placeVerifier,
            new PlaceAnalysisProperties(
                    true, 100, 20, 1, true, 600, 300, 3,
                    Duration.ofSeconds(5), 20, 8, 12
            ),
            Clock.fixed(NOW, Clock.systemDefaultZone().getZone()),
            Runnable::run
    );

    @BeforeEach
    void setUp() {
        when(importRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.of(receivedImport(invocation.getArgument(0))));
        when(metadataService.fetch(anyString(), any(ContentSourceType.class))).thenReturn(metadata());
        when(resultWriter.reuseUnchangedContent(anyLong(), anyString(), any(ContentMetadata.class)))
                .thenReturn(false);
        when(resultWriter.saveMetadata(anyLong(), anyString(), any(ContentMetadata.class))).thenReturn(30L);
        when(resultWriter.loadCachedAnalysis(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(resultWriter.claimAnalysis(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn("analysis-claim-1");
        when(resultWriter.saveAnalysis(anyLong(), anyString(), anyString(), anyString(), anyString(), anyList(), any()))
                .thenReturn(true);
        when(placeAnalyzer.modelKey()).thenReturn("gemini-test");
        when(placeAnalyzer.promptVersion()).thenReturn("place-extraction-test");
        when(reservationService.heartbeatClaim(anyLong(), anyString(), any())).thenReturn(true);
        when(reservationService.failClaimed(anyLong(), anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void savesVerifiedCandidatesWhenSomeVerificationsFail() {
        ExtractedPlace succeeded = new ExtractedPlace("성공 카페", null, null, "EXPLICIT_VENUE");
        ExtractedPlace failed = new ExtractedPlace("실패 카페", null, null, "EXPLICIT_VENUE");
        when(placeAnalyzer.analyze(any(ContentMetadata.class))).thenReturn(List.of(succeeded, failed));
        when(placeVerifier.verify(succeeded)).thenReturn(new PlaceVerificationResult(
                verifiedPlace("100", "성공 카페"),
                PlaceVerificationStatus.VERIFIED
        ));
        when(placeVerifier.verify(failed)).thenThrow(new PlaceVerificationUnavailableException());

        service.processClaimed(1L, CLAIM_TOKEN);

        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(resultWriter).saveSuccess(
                eq(1L), eq(CLAIM_TOKEN), any(ContentMetadata.class), captor.capture(), eq(true)
        );
        List<VerifiedCandidate> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().extracted().name()).isEqualTo("성공 카페");
    }

    @Test
    void failsImportWhenEveryVerificationFails() {
        ExtractedPlace first = new ExtractedPlace("실패 카페", null, null, "EXPLICIT_VENUE");
        ExtractedPlace second = new ExtractedPlace("실패 펜션", null, null, "EXPLICIT_VENUE");
        when(placeAnalyzer.analyze(any(ContentMetadata.class))).thenReturn(List.of(first, second));
        when(placeVerifier.verify(any(ExtractedPlace.class)))
                .thenThrow(new PlaceVerificationUnavailableException());

        service.processClaimed(1L, CLAIM_TOKEN);

        org.mockito.Mockito.verify(reservationService).failClaimed(
                eq(1L),
                eq(CLAIM_TOKEN),
                eq(kr.omong.dulpick.global.exception.ErrorCode.PLACE_VERIFICATION_UNAVAILABLE.getCode()),
                any(Instant.class)
        );
    }

    private PlaceImport receivedImport(Long importId) {
        PlaceImport placeImport = PlaceImport.receive(
                1L,
                "https://www.instagram.com/reel/example",
                "url-hash",
                ContentSourceType.INSTAGRAM_REEL,
                NOW
        );
        placeImport.start(NOW);
        ReflectionTestUtils.setField(placeImport, "id", importId);
        ReflectionTestUtils.setField(placeImport, "processingClaimToken", CLAIM_TOKEN);
        return placeImport;
    }

    private ContentMetadata metadata() {
        return new ContentMetadata(
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                "제목",
                "본문",
                "",
                "content-hash",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private VerifiedPlace verifiedPlace(String kakaoPlaceId, String name) {
        return new VerifiedPlace(
                kakaoPlaceId,
                name,
                "서울 강남구",
                "서울 강남구 테헤란로",
                null,
                null,
                "CE7",
                "음식점 > 카페",
                null
        );
    }
}
