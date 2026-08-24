package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceImportServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final String IMPORT_CLAIM_TOKEN = "import-claim-token";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PlaceImportRepository importRepository = mock(PlaceImportRepository.class);
    private final PlaceCandidateRepository candidateRepository = mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final PlaceImportResultWriter resultWriter = mock(PlaceImportResultWriter.class);
    private final PlaceImageEnrichmentService imageEnrichmentService =
            mock(PlaceImageEnrichmentService.class);
    private final ContentImageEnrichmentService contentImageEnrichmentService =
            mock(ContentImageEnrichmentService.class);
    private final PlaceImportReservationService reservationService =
            mock(PlaceImportReservationService.class);
    private final ContentSourceUrlParser urlParser = mock(ContentSourceUrlParser.class);
    private final MetadataService metadataService = mock(MetadataService.class);
    private final PlaceAnalyzer placeAnalyzer = mock(PlaceAnalyzer.class);
    private final PlaceVerifier placeVerifier = mock(PlaceVerifier.class);
    private final PlaceAnalysisProperties properties = new PlaceAnalysisProperties(
            true,
            100,
            10,
            1,
            false,
            600,
            300,
            3,
            Duration.ofSeconds(5),
            20,
            2,
            3
    );
    private final PlaceImportViewMapper viewMapper = new PlaceImportViewMapper(
            candidateRepository,
            placeRepository,
            memberPlaceRepository,
            properties
    );
    private final PlaceImportQueryService queryService = new PlaceImportQueryService(
            importRepository,
            viewMapper
    );
    private final PlaceImportService service = new PlaceImportService(
            memberRepository,
            importRepository,
            viewMapper,
            reservationService,
            urlParser,
            properties,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );
    private final PlaceImportProcessingService processingService = createProcessingService(Runnable::run);

    @Test
    void queuesNewImportWithoutCallingExternalProviders() {
        String rawUrl = "https://www.instagram.com/reel/example?igsh=tracking";
        String canonicalUrl = "https://www.instagram.com/reel/example";
        Member member = mock(Member.class);
        PlaceImport placeImport = receivedImport();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(member.isActive()).thenReturn(true);
        when(member.getId()).thenReturn(1L);
        when(urlParser.parse(rawUrl)).thenReturn(new ContentSourceUrlParser.ParsedSource(
                canonicalUrl,
                ContentSourceType.INSTAGRAM_REEL
        ));
        when(importRepository.findByMemberIdAndCanonicalUrlHash(eq(1L), any()))
                .thenReturn(Optional.empty());
        when(reservationService.reserve(
                eq(1L),
                eq(canonicalUrl),
                any(),
                eq(ContentSourceType.INSTAGRAM_REEL),
                eq(NOW)
        )).thenReturn(new PlaceImportReservationService.Reservation(1L));
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L)).thenReturn(List.of());

        PlaceImportSubmissionView submission = service.importLink(1L, rawUrl);

        assertThat(submission.placeImport().status()).isEqualTo(PlaceImportStatus.RECEIVED);
        assertThat(submission.placeImport().nextAction()).isEqualTo(PlaceImportNextAction.WAIT);
        verifyNoInteractions(metadataService, placeAnalyzer, placeVerifier, resultWriter);
    }

    @Test
    void returnsCachedCompletedImportWithoutCallingExternalProviders() {
        String rawUrl = "https://www.instagram.com/reel/example";
        Member member = mock(Member.class);
        PlaceImport placeImport = receivedImport();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(member.isActive()).thenReturn(true);
        when(urlParser.parse(rawUrl)).thenReturn(new ContentSourceUrlParser.ParsedSource(
                rawUrl,
                ContentSourceType.INSTAGRAM_REEL
        ));
        when(importRepository.findByMemberIdAndCanonicalUrlHash(eq(1L), any()))
                .thenReturn(Optional.of(placeImport));
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.REVIEW_REQUIRED);
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L)).thenReturn(List.of());

        PlaceImportSubmissionView submission = service.importLink(1L, rawUrl);

        assertThat(submission.placeImport().status()).isEqualTo(PlaceImportStatus.REVIEW_REQUIRED);
        assertThat(submission.placeImport().nextAction())
                .isEqualTo(PlaceImportNextAction.SELECT_PLACES);
        verifyNoInteractions(metadataService, placeAnalyzer, placeVerifier, resultWriter);
    }

    @Test
    void reclaimsProcessingImportOnlyAfterStaleTimeout() {
        PlaceImport placeImport = receivedImport();
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.PROCESSING);
        when(placeImport.getUpdatedAt()).thenReturn(NOW.minusSeconds(601));
        when(reservationService.claimPending(
                1L,
                NOW,
                NOW.minusSeconds(600)
        )).thenReturn(null);

        assertThat(processingService.claimPending(1L)).isNull();

        verify(reservationService).claimPending(
                1L,
                NOW,
                NOW.minusSeconds(600)
        );
        verifyNoInteractions(metadataService, placeAnalyzer, placeVerifier, resultWriter);
    }

    @Test
    void ignoresProcessingRequestWhenImportClaimTokenDoesNotMatch() {
        PlaceImport placeImport = receivedImport();
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.PROCESSING);
        when(placeImport.getProcessingClaimToken()).thenReturn("new-claim-token");
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));

        processingService.processClaimed(1L, "stale-claim-token");

        verifyNoInteractions(
                metadataService,
                placeAnalyzer,
                placeVerifier,
                resultWriter,
                reservationService
        );
    }

    @Test
    void reusesGeminiCandidatesWhenKakaoVerificationRetries() {
        PlaceImport placeImport = receivedImport();
        when(placeImport.getStatus())
                .thenReturn(PlaceImportStatus.RECEIVED)
                .thenReturn(PlaceImportStatus.PROCESSING);
        ContentMetadata metadata = metadata();
        ExtractedPlace extractedPlace = new ExtractedPlace(
                "밀빛 망원점",
                "서울 마포구",
                "망원 카페 - 밀빛",
                "EXPLICIT_VENUE"
        );
        VerifiedPlace verifiedPlace = new VerifiedPlace(
                "kakao-place-id",
                "밀빛 망원점",
                "서울 마포구 망원동",
                "서울 마포구 희우정로16길 25",
                new BigDecimal("37.5546637"),
                new BigDecimal("126.9033951"),
                "FD6",
                "음식점 > 제과,베이커리",
                null
        );
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(reservationService.claimPending(eq(1L), any(), any()))
                .thenReturn(IMPORT_CLAIM_TOKEN);
        when(reservationService.heartbeatClaim(1L, IMPORT_CLAIM_TOKEN, NOW)).thenReturn(true);
        when(metadataService.fetch(placeImport.getCanonicalUrl(), ContentSourceType.INSTAGRAM_REEL))
                .thenReturn(metadata);
        when(resultWriter.reuseUnchangedContent(1L, IMPORT_CLAIM_TOKEN, metadata))
                .thenReturn(false);
        when(resultWriter.saveMetadata(1L, IMPORT_CLAIM_TOKEN, metadata)).thenReturn(10L);
        when(placeAnalyzer.modelKey()).thenReturn("gemini-2.5-flash");
        when(placeAnalyzer.promptVersion()).thenReturn("place-extraction-v3");
        when(resultWriter.loadCachedAnalysis(
                10L,
                metadata.contentHash(),
                "gemini-2.5-flash",
                "place-extraction-v3"
        )).thenReturn(Optional.empty())
                .thenReturn(Optional.of(List.of(extractedPlace)));
        when(resultWriter.claimAnalysis(eq(10L), any(), any(), any(), any(), any()))
                .thenReturn("claim-token");
        when(placeAnalyzer.analyze(metadata)).thenReturn(List.of(extractedPlace));
        when(resultWriter.saveAnalysis(
                eq(10L),
                eq("claim-token"),
                eq(metadata.contentHash()),
                eq("gemini-2.5-flash"),
                eq("place-extraction-v3"),
                eq(List.of(extractedPlace)),
                any()
        )).thenReturn(true);
        when(placeVerifier.verify(extractedPlace))
                .thenThrow(new PlaceVerificationUnavailableException())
                .thenReturn(new PlaceVerificationResult(
                        verifiedPlace,
                        PlaceVerificationStatus.VERIFIED
                ));

        assertThat(processingService.claimPending(1L)).isEqualTo(IMPORT_CLAIM_TOKEN);
        processingService.processClaimed(1L, IMPORT_CLAIM_TOKEN);

        verify(placeAnalyzer, times(1)).analyze(metadata);
        verify(placeVerifier, times(2)).verify(extractedPlace);
        verify(resultWriter).saveSuccess(
                1L,
                IMPORT_CLAIM_TOKEN,
                metadata,
                List.of(new VerifiedCandidate(
                        extractedPlace,
                        verifiedPlace,
                        PlaceVerificationStatus.VERIFIED
                ))
        );
    }

    @Test
    void verifiesCandidatesConcurrentlyWithoutChangingCandidateOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            PlaceImportProcessingService parallelService = createProcessingService(executor);
            PlaceImport placeImport = receivedImport();
            when(placeImport.getStatus()).thenReturn(PlaceImportStatus.PROCESSING);
            when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
            ContentMetadata metadata = metadata();
            ExtractedPlace first = new ExtractedPlace("첫 번째 장소", "서울", "첫 번째", "EXPLICIT_VENUE");
            ExtractedPlace second = new ExtractedPlace("두 번째 장소", "서울", "두 번째", "EXPLICIT_VENUE");
            VerifiedPlace firstVerified = verifiedPlace("first-id", first.name());
            VerifiedPlace secondVerified = verifiedPlace("second-id", second.name());
            CountDownLatch started = new CountDownLatch(2);
            when(metadataService.fetch(placeImport.getCanonicalUrl(), ContentSourceType.INSTAGRAM_REEL))
                    .thenReturn(metadata);
            when(resultWriter.reuseUnchangedContent(1L, IMPORT_CLAIM_TOKEN, metadata)).thenReturn(false);
            when(resultWriter.saveMetadata(1L, IMPORT_CLAIM_TOKEN, metadata)).thenReturn(10L);
            when(placeAnalyzer.modelKey()).thenReturn("gemini-3.5-flash-lite");
            when(placeAnalyzer.promptVersion()).thenReturn("place-extraction-v3");
            when(resultWriter.loadCachedAnalysis(
                    10L, metadata.contentHash(), "gemini-3.5-flash-lite", "place-extraction-v3"
            )).thenReturn(Optional.of(List.of(first, second)));
            when(placeRepository.findFirstByNameAndAddressHint(any(), eq("서울")))
                    .thenReturn(Optional.empty());
            when(placeVerifier.verify(any())).thenAnswer(invocation -> {
                ExtractedPlace extracted = invocation.getArgument(0);
                started.countDown();
                if (!started.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("place verifications were not started concurrently");
                }
                VerifiedPlace verified = extracted.equals(first) ? firstVerified : secondVerified;
                return new PlaceVerificationResult(verified, PlaceVerificationStatus.VERIFIED);
            });

            parallelService.processClaimed(1L, IMPORT_CLAIM_TOKEN);

            verify(resultWriter).saveSuccess(
                    1L,
                    IMPORT_CLAIM_TOKEN,
                    metadata,
                    List.of(
                            new VerifiedCandidate(first, firstVerified, PlaceVerificationStatus.VERIFIED),
                            new VerifiedCandidate(second, secondVerified, PlaceVerificationStatus.VERIFIED)
                    )
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void requeuesImportWithoutCallingGeminiWhenAnotherRequestOwnsAnalysis() {
        PlaceImport placeImport = receivedImport();
        when(placeImport.getStatus())
                .thenReturn(PlaceImportStatus.RECEIVED)
                .thenReturn(PlaceImportStatus.PROCESSING);
        ContentMetadata metadata = metadata();
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(reservationService.claimPending(eq(1L), any(), any()))
                .thenReturn(IMPORT_CLAIM_TOKEN);
        when(metadataService.fetch(placeImport.getCanonicalUrl(), ContentSourceType.INSTAGRAM_REEL))
                .thenReturn(metadata);
        when(resultWriter.reuseUnchangedContent(1L, IMPORT_CLAIM_TOKEN, metadata))
                .thenReturn(false);
        when(resultWriter.saveMetadata(1L, IMPORT_CLAIM_TOKEN, metadata)).thenReturn(10L);
        when(placeAnalyzer.modelKey()).thenReturn("gemini-2.5-flash");
        when(placeAnalyzer.promptVersion()).thenReturn("place-extraction-v3");
        when(resultWriter.loadCachedAnalysis(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(String.class)
        ))
                .thenReturn(Optional.empty());
        when(resultWriter.claimAnalysis(
                any(Long.class),
                any(String.class),
                any(String.class),
                any(String.class),
                any(Instant.class),
                any(Instant.class)
        ))
                .thenReturn(null);

        assertThat(processingService.claimPending(1L)).isEqualTo(IMPORT_CLAIM_TOKEN);
        processingService.processClaimed(1L, IMPORT_CLAIM_TOKEN);

        verify(reservationService).requeueClaimed(1L, IMPORT_CLAIM_TOKEN, NOW);
        verifyNoInteractions(placeVerifier);
        verify(placeAnalyzer, never()).analyze(any(ContentMetadata.class));
    }

    @Test
    void returnsExtractedCandidateWithoutLookingUpNullPlaceId() {
        PlaceImport placeImport = receivedImport();
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        when(placeImport.getMemberId()).thenReturn(1L);
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.FAILED);
        when(placeImport.getFailureCode()).thenReturn("PLACE_NOT_VERIFIED");
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L))
                .thenReturn(List.of(candidate));
        when(candidate.getId()).thenReturn(100L);
        when(candidate.getPlaceId()).thenReturn(null);
        when(candidate.getVerificationStatus()).thenReturn(PlaceVerificationStatus.EXTRACTED);
        when(candidate.getExtractedName()).thenReturn("밀빛 망원점");
        when(candidate.getExtractedAddressHint()).thenReturn("서울 마포구");

        PlaceImportView view = queryService.get(1L, 1L);

        assertThat(view.candidates()).singleElement().satisfies(result -> {
            assertThat(result.verificationStatus()).isEqualTo(PlaceVerificationStatus.EXTRACTED);
            assertThat(result.extractedName()).isEqualTo("밀빛 망원점");
            assertThat(result.place()).isNull();
        });
        verifyNoInteractions(placeRepository);
    }

    @Test
    void marksVerifiedCandidateAlreadySavedByCurrentMember() {
        PlaceImport placeImport = receivedImport();
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        MemberPlace memberPlace = mock(MemberPlace.class);
        when(placeImport.getMemberId()).thenReturn(1L);
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.REVIEW_REQUIRED);
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L))
                .thenReturn(List.of(candidate));
        when(candidate.getId()).thenReturn(100L);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(candidate.getVerificationStatus()).thenReturn(PlaceVerificationStatus.VERIFIED);
        when(place.getId()).thenReturn(20L);
        when(placeRepository.findAllById(List.of(20L))).thenReturn(List.of(place));
        when(memberPlace.getPlace()).thenReturn(place);
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(1L, List.of(20L)))
                .thenReturn(List.of(memberPlace));

        PlaceImportView view = queryService.get(1L, 1L);

        assertThat(view.candidates()).singleElement().satisfies(result ->
                assertThat(result.place().savedByMe()).isTrue()
        );
    }

    @Test
    void verifiesNaverMapPlaceWithoutCallingGemini() {
        PlaceImport placeImport = receivedImport();
        ContentMetadata metadata = new ContentMetadata(
                "https://naver.me/F1r21MEx",
                ContentSourceType.NAVER_SHORT_LINK,
                "을지식당",
                "서울 중구 을지로40길 17",
                null,
                "naver-content-hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ExtractedPlace extractedPlace = new ExtractedPlace(
                "을지식당",
                "서울 중구 을지로40길 17",
                "을지식당 서울 중구 을지로40길 17",
                "EXPLICIT_VENUE"
        );
        VerifiedPlace verifiedPlace = new VerifiedPlace(
                "18699959",
                "을지식당",
                "서울 중구 을지로6가 67-3",
                "서울 중구 을지로40길 17",
                new BigDecimal("37.5659000"),
                new BigDecimal("127.0044624"),
                "FD6",
                "음식점 > 한식",
                null
        );
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.PROCESSING);
        when(placeImport.getProcessingClaimToken()).thenReturn(IMPORT_CLAIM_TOKEN);
        when(placeImport.getSourceType()).thenReturn(ContentSourceType.NAVER_SHORT_LINK);
        when(placeImport.getCanonicalUrl()).thenReturn("https://naver.me/F1r21MEx");
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(metadataService.fetch(
                "https://naver.me/F1r21MEx",
                ContentSourceType.NAVER_SHORT_LINK
        )).thenReturn(metadata);
        when(placeVerifier.verify(extractedPlace)).thenReturn(new PlaceVerificationResult(
                verifiedPlace,
                PlaceVerificationStatus.VERIFIED
        ));

        processingService.processClaimed(1L, IMPORT_CLAIM_TOKEN);

        verify(placeAnalyzer, never()).analyze(any(ContentMetadata.class));
        verify(resultWriter).saveExtractedCandidates(
                1L,
                IMPORT_CLAIM_TOKEN,
                List.of(extractedPlace)
        );
        verify(resultWriter).saveSuccess(
                1L,
                IMPORT_CLAIM_TOKEN,
                metadata,
                List.of(new VerifiedCandidate(
                        extractedPlace,
                        verifiedPlace,
                        PlaceVerificationStatus.VERIFIED
                ))
        );
    }

    @Test
    void reusesExistingPlaceWithoutCallingKakao() {
        PlaceImport placeImport = receivedImport();
        ContentMetadata metadata = new ContentMetadata(
                "https://naver.me/F1r21MEx",
                ContentSourceType.NAVER_SHORT_LINK,
                "을지식당",
                "서울 중구 을지로40길 17",
                null,
                "naver-content-hash",
                NOW,
                null,
                null,
                null,
                null,
                null,
                null
        );
        ExtractedPlace extractedPlace = new ExtractedPlace(
                "을지식당",
                "서울 중구 을지로40길 17",
                "을지식당 서울 중구 을지로40길 17",
                "EXPLICIT_VENUE"
        );
        Place cachedPlace = mock(Place.class);
        when(cachedPlace.getKakaoPlaceId()).thenReturn("18699959");
        when(cachedPlace.getName()).thenReturn("을지식당");
        when(cachedPlace.getAddress()).thenReturn("서울 중구 을지로6가 67-3");
        when(cachedPlace.getRoadAddress()).thenReturn("서울 중구 을지로40길 17");
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.PROCESSING);
        when(placeImport.getProcessingClaimToken()).thenReturn(IMPORT_CLAIM_TOKEN);
        when(placeImport.getSourceType()).thenReturn(ContentSourceType.NAVER_SHORT_LINK);
        when(placeImport.getCanonicalUrl()).thenReturn("https://naver.me/F1r21MEx");
        when(importRepository.findById(1L)).thenReturn(Optional.of(placeImport));
        when(metadataService.fetch("https://naver.me/F1r21MEx", ContentSourceType.NAVER_SHORT_LINK))
                .thenReturn(metadata);
        when(placeRepository.findFirstByNameAndAddressHint(
                extractedPlace.name(), extractedPlace.addressHint()
        )).thenReturn(Optional.of(cachedPlace));

        processingService.processClaimed(1L, IMPORT_CLAIM_TOKEN);

        verifyNoInteractions(placeVerifier);
        verify(resultWriter).saveSuccess(
                eq(1L),
                eq(IMPORT_CLAIM_TOKEN),
                eq(metadata),
                any()
        );
    }

    private PlaceImport receivedImport() {
        PlaceImport placeImport = mock(PlaceImport.class);
        when(placeImport.getId()).thenReturn(1L);
        when(placeImport.getStatus()).thenReturn(PlaceImportStatus.RECEIVED);
        when(placeImport.getSourceType()).thenReturn(ContentSourceType.INSTAGRAM_REEL);
        when(placeImport.getCanonicalUrl()).thenReturn("https://www.instagram.com/reel/example");
        when(placeImport.getContentId()).thenReturn(10L);
        when(placeImport.getProcessingClaimToken()).thenReturn(IMPORT_CLAIM_TOKEN);
        return placeImport;
    }

    private PlaceImportProcessingService createProcessingService(java.util.concurrent.Executor executor) {
        return new PlaceImportProcessingService(
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
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC),
                executor
        );
    }

    private VerifiedPlace verifiedPlace(String kakaoPlaceId, String name) {
        return new VerifiedPlace(
                kakaoPlaceId,
                name,
                "서울 중구 주소",
                "서울 중구 도로명",
                new BigDecimal("37.5659000"),
                new BigDecimal("127.0044624"),
                "FD6",
                "음식점 > 한식",
                null
        );
    }

    private ContentMetadata metadata() {
        return new ContentMetadata(
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                "망원동 빵지순례",
                "망원 카페 - 밀빛",
                null,
                "content-hash",
                NOW,
                "찐",
                "jjin_.record",
                LocalDate.of(2026, 8, 7),
                1_833L,
                76L,
                NOW
        );
    }
}
