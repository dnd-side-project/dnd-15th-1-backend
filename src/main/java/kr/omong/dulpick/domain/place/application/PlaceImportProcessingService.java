package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PlaceImportProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportProcessingService.class);

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImportResultWriter resultWriter;
    private final PlaceImageEnrichmentService imageEnrichmentService;
    private final PlaceImportReservationService reservationService;
    private final MetadataService metadataService;
    private final PlaceAnalyzer placeAnalyzer;
    private final PlaceVerifier placeVerifier;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;

    public PlaceImportProcessingService(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImportResultWriter resultWriter,
            PlaceImageEnrichmentService imageEnrichmentService,
            PlaceImportReservationService reservationService,
            MetadataService metadataService,
            PlaceAnalyzer placeAnalyzer,
            PlaceVerifier placeVerifier,
            PlaceAnalysisProperties properties,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.resultWriter = resultWriter;
        this.imageEnrichmentService = imageEnrichmentService;
        this.reservationService = reservationService;
        this.metadataService = metadataService;
        this.placeAnalyzer = placeAnalyzer;
        this.placeVerifier = placeVerifier;
        this.properties = properties;
        this.clock = clock;
    }

    public String claimPending(Long importId) {
        PlaceImport placeImport = importRepository.findById(importId).orElse(null);
        if (!isRecoverable(placeImport)) {
            return null;
        }
        Instant now = clock.instant();
        return reservationService.claimPending(
                importId,
                now,
                now.minusSeconds(properties.staleTimeoutSeconds())
        );
    }

    public void processClaimed(Long importId, String claimToken) {
        PlaceImport placeImport = importRepository.findById(importId).orElse(null);
        if (placeImport == null
                || placeImport.getStatus() != PlaceImportStatus.PROCESSING
                || !Objects.equals(claimToken, placeImport.getProcessingClaimToken())) {
            return;
        }
        processWithRetry(placeImport, placeImport.getSourceType(), claimToken);
    }

    private boolean isRecoverable(PlaceImport placeImport) {
        if (placeImport == null) {
            return false;
        }
        if (placeImport.getStatus() == PlaceImportStatus.RECEIVED) {
            return true;
        }
        return placeImport.getStatus() == PlaceImportStatus.PROCESSING
                && placeImport.getUpdatedAt()
                .plusSeconds(properties.staleTimeoutSeconds())
                .isBefore(clock.instant());
    }

    private void processWithRetry(PlaceImport placeImport, ContentSourceType sourceType, String claimToken) {
        int attempts = properties.maxRetries() + 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                process(placeImport, sourceType, claimToken);
                return;
            } catch (PlaceImportClaimLostException exception) {
                logger.info("Place import claim lost: importId={}", placeImport.getId());
                return;
            } catch (BusinessException exception) {
                if (!canRetryImmediately(exception) || attempt == attempts - 1) {
                    reservationService.failClaimed(placeImport.getId(), claimToken,
                            exception.getErrorCode().getCode(), clock.instant());
                    return;
                }
                if (!reservationService.heartbeatClaim(placeImport.getId(), claimToken, clock.instant())) {
                    return;
                }
            } catch (RuntimeException exception) {
                logger.error("Place import processing failed: importId={}", placeImport.getId(), exception);
                reservationService.failClaimed(placeImport.getId(), claimToken,
                        ErrorCode.INTERNAL_ERROR.getCode(), clock.instant());
                return;
            }
        }
    }

    private boolean canRetryImmediately(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.PLACE_ANALYSIS_UNAVAILABLE
                || exception.getErrorCode() == ErrorCode.PLACE_VERIFICATION_UNAVAILABLE;
    }

    private void process(PlaceImport placeImport, ContentSourceType sourceType, String claimToken) {
        ContentMetadata metadata = metadataService.fetch(placeImport.getCanonicalUrl(), sourceType);
        if (sourceType.storesPublicContent()) {
            if (resultWriter.reuseUnchangedContent(placeImport.getId(), claimToken, metadata)) {
                imageEnrichmentService.enrichImportPlaces(placeImport.getId());
                return;
            }
            placeImport.attachContent(resultWriter.saveMetadata(placeImport.getId(), claimToken, metadata));
        } else {
            placeImport.recordMetadata(metadata.title(), metadata.caption(), metadata.thumbnailUrl(),
                    metadata.contentHash(), metadata.sourceUpdatedAt());
            placeImport.recordSourceMetadata(metadata.sourceAuthorName(), metadata.sourceAuthorUsername(),
                    metadata.sourcePublishedOn(), metadata.likeCount(), metadata.commentCount(),
                    metadata.engagementCheckedAt());
        }
        String sourceText = normalizeText(metadata.title() + " " + metadata.caption());
        String model = placeAnalyzer.modelKey();
        String prompt = placeAnalyzer.promptVersion();
        List<ExtractedPlace> extracted = isNaverPlace(sourceType)
                ? List.of(new ExtractedPlace(metadata.title(), metadata.caption(),
                metadata.title() + " " + metadata.caption(), "EXPLICIT_VENUE"))
                : loadCachedCandidates(placeImport, metadata, model, prompt);
        if (extracted == null) {
            extracted = analyzeWithGemini(placeImport, metadata, sourceText, model, prompt, claimToken);
            if (extracted == null) {
                return;
            }
        }
        if (isNaverPlace(sourceType)) {
            resultWriter.saveExtractedCandidates(placeImport.getId(), claimToken, extracted);
        }
        List<VerifiedCandidate> candidates = new ArrayList<>();
        for (ExtractedPlace extractedPlace : extracted) {
            PlaceVerificationResult verification = verifyCachedOrExternal(extractedPlace);
            if (verification != null) {
                candidates.add(new VerifiedCandidate(extractedPlace, verification.place(), verification.status()));
            }
        }
        if (candidates.isEmpty()) {
            reservationService.failClaimed(placeImport.getId(), claimToken,
                    ErrorCode.PLACE_NOT_VERIFIED.getCode(), clock.instant());
            return;
        }
        resultWriter.saveSuccess(placeImport.getId(), claimToken, metadata, candidates);
        imageEnrichmentService.enrichImportPlaces(placeImport.getId());
    }

    private List<ExtractedPlace> analyzeWithGemini(PlaceImport placeImport, ContentMetadata metadata,
                                                    String sourceText, String model, String prompt,
                                                    String claimToken) {
        Long contentId = placeImport.getContentId();
        if (contentId == null) {
            List<ExtractedPlace> extracted = analyze(metadata, sourceText);
            resultWriter.saveExtractedCandidates(placeImport.getId(), claimToken, extracted);
            return extracted;
        }
        String analysisClaim = resultWriter.claimAnalysis(contentId, metadata.contentHash(), model, prompt,
                clock.instant(), clock.instant().minusSeconds(properties.staleTimeoutSeconds()));
        if (analysisClaim == null) {
            List<ExtractedPlace> cached = loadCachedCandidates(placeImport, metadata, model, prompt);
            if (cached == null) {
                reservationService.requeueClaimed(placeImport.getId(), claimToken, clock.instant());
            }
            return cached;
        }
        try {
            List<ExtractedPlace> extracted = analyze(metadata, sourceText);
            if (!resultWriter.saveAnalysis(contentId, analysisClaim, metadata.contentHash(), model, prompt,
                    extracted, clock.instant())) {
                reservationService.requeueClaimed(placeImport.getId(), claimToken, clock.instant());
                return null;
            }
            resultWriter.saveExtractedCandidates(placeImport.getId(), claimToken, extracted);
            return extracted;
        } catch (RuntimeException exception) {
            resultWriter.failAnalysis(contentId, analysisClaim);
            throw exception;
        }
    }

    private List<ExtractedPlace> analyze(ContentMetadata metadata, String sourceText) {
        return placeAnalyzer.analyze(metadata).stream()
                .map(candidate -> validateEvidence(candidate, sourceText))
                .limit(properties.maxCandidates())
                .toList();
    }

    private PlaceVerificationResult verifyCachedOrExternal(ExtractedPlace extractedPlace) {
        if (extractedPlace.addressHint() != null && !extractedPlace.addressHint().isBlank()) {
            PlaceVerificationResult cached = placeRepository
                    .findFirstByNameAndAddressHint(extractedPlace.name(), extractedPlace.addressHint())
                    .map(place -> new PlaceVerificationResult(
                            new VerifiedPlace(
                                    place.getKakaoPlaceId(),
                                    place.getName(),
                                    place.getAddress(),
                                    place.getRoadAddress(),
                                    place.getLatitude(),
                                    place.getLongitude(),
                                    place.getCategoryGroupCode(),
                                    place.getCategory(),
                                    place.getPhone(),
                                    place.getKakaoPlaceUrl(),
                                    place.getThumbnailUrl()
                            ),
                            PlaceVerificationStatus.VERIFIED
                    ))
                    .orElse(null);
            if (cached != null) {
                return cached;
            }
        }
        return placeVerifier.verify(extractedPlace);
    }

    private List<ExtractedPlace> loadCachedCandidates(PlaceImport placeImport, ContentMetadata metadata,
                                                       String model, String prompt) {
        if (placeImport.getContentId() != null) {
            return resultWriter.loadCachedAnalysis(placeImport.getContentId(), metadata.contentHash(), model, prompt)
                    .orElse(null);
        }
        List<ExtractedPlace> candidates = candidateRepository.findAllByImportIdOrderByIdAsc(placeImport.getId()).stream()
                .filter(candidate -> candidate.getVerificationStatus() == PlaceVerificationStatus.EXTRACTED)
                .map(candidate -> new ExtractedPlace(candidate.getExtractedName(), candidate.getExtractedAddressHint(),
                        candidate.getEvidence(), candidate.getMentionType()))
                .toList();
        return candidates.isEmpty() ? null : candidates;
    }

    private ExtractedPlace validateEvidence(ExtractedPlace candidate, String sourceText) {
        if (candidate.evidence() == null || candidate.evidence().isBlank()) {
            return candidate;
        }
        if (sourceText.contains(normalizeText(candidate.evidence()))) {
            return candidate;
        }
        return new ExtractedPlace(candidate.name(), candidate.addressHint(), null, candidate.mentionType());
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }

    private boolean isNaverPlace(ContentSourceType sourceType) {
        return sourceType == ContentSourceType.NAVER_SHORT_LINK || sourceType == ContentSourceType.NAVER_MAP;
    }
}
