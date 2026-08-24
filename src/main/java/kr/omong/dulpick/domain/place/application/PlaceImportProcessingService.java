package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import kr.omong.dulpick.domain.place.application.exception.PlaceVerificationUnavailableException;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

@Service
public class PlaceImportProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportProcessingService.class);

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final PlaceImportResultWriter resultWriter;
    private final PlaceImageEnrichmentService imageEnrichmentService;
    private final PlaceImageEnrichmentDispatcher imageEnrichmentDispatcher;
    private final ContentImageEnrichmentService contentImageEnrichmentService;
    private final PlaceImportReservationService reservationService;
    private final MetadataService metadataService;
    private final PlaceAnalyzer placeAnalyzer;
    private final PlaceVerifier placeVerifier;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;
    private final Executor verificationExecutor;

    public PlaceImportProcessingService(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImportResultWriter resultWriter,
            PlaceImageEnrichmentService imageEnrichmentService,
            ContentImageEnrichmentService contentImageEnrichmentService,
            PlaceImportReservationService reservationService,
            MetadataService metadataService,
            PlaceAnalyzer placeAnalyzer,
            PlaceVerifier placeVerifier,
            PlaceAnalysisProperties properties,
            Clock clock,
            @Qualifier("placeVerificationExecutor")
            Executor verificationExecutor
    ) {
        this(
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
                clock,
                verificationExecutor,
                null
        );
    }

    @Autowired
    public PlaceImportProcessingService(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImportResultWriter resultWriter,
            PlaceImageEnrichmentService imageEnrichmentService,
            ContentImageEnrichmentService contentImageEnrichmentService,
            PlaceImageEnrichmentDispatcher imageEnrichmentDispatcher,
            PlaceImportReservationService reservationService,
            MetadataService metadataService,
            PlaceAnalyzer placeAnalyzer,
            PlaceVerifier placeVerifier,
            PlaceAnalysisProperties properties,
            Clock clock,
            @Qualifier("placeVerificationExecutor")
            Executor verificationExecutor
    ) {
        this(
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
                clock,
                verificationExecutor,
                imageEnrichmentDispatcher
        );
    }

    private PlaceImportProcessingService(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            PlaceImportResultWriter resultWriter,
            PlaceImageEnrichmentService imageEnrichmentService,
            ContentImageEnrichmentService contentImageEnrichmentService,
            PlaceImportReservationService reservationService,
            MetadataService metadataService,
            PlaceAnalyzer placeAnalyzer,
            PlaceVerifier placeVerifier,
            PlaceAnalysisProperties properties,
            Clock clock,
            Executor verificationExecutor,
            PlaceImageEnrichmentDispatcher imageEnrichmentDispatcher
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.resultWriter = resultWriter;
        this.imageEnrichmentService = imageEnrichmentService;
        this.imageEnrichmentDispatcher = imageEnrichmentDispatcher;
        this.contentImageEnrichmentService = contentImageEnrichmentService;
        this.reservationService = reservationService;
        this.metadataService = metadataService;
        this.placeAnalyzer = placeAnalyzer;
        this.placeVerifier = placeVerifier;
        this.properties = properties;
        this.clock = clock;
        this.verificationExecutor = verificationExecutor;
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
        processClaimed(importId, claimToken, clock.instant());
    }

    public void processClaimed(Long importId, String claimToken, Instant queuedAt) {
        PlaceImport placeImport = importRepository.findById(importId).orElse(null);
        if (placeImport == null
                || placeImport.getStatus() != PlaceImportStatus.PROCESSING
                || !Objects.equals(claimToken, placeImport.getProcessingClaimToken())) {
            return;
        }
        processWithRetry(placeImport, placeImport.getSourceType(), claimToken, queuedAt);
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

    private void processWithRetry(
            PlaceImport placeImport,
            ContentSourceType sourceType,
            String claimToken,
            Instant queuedAt
        ) {
        int attempts = properties.maxRetries() + 1;
        ProcessingTiming timing = new ProcessingTiming();
        long queueWaitMillis = queueWaitMillis(queuedAt);
        long totalStartedAt = System.nanoTime();
        try {
            for (int attempt = 0; attempt < attempts; attempt++) {
                try {
                    process(placeImport, sourceType, claimToken, timing);
                    return;
                } catch (PlaceImportClaimLostException exception) {
                    logger.info("Place import claim lost: importId={}", placeImport.getId());
                    return;
                } catch (BusinessException exception) {
                    if (!canRetryImmediately(exception) || attempt == attempts - 1) {
                        measureDbWrite(
                                () -> reservationService.failClaimed(
                                        placeImport.getId(), claimToken,
                                        exception.getErrorCode().getCode(), clock.instant()
                                ),
                                timing
                        );
                        return;
                    }
                    boolean heartbeatSucceeded = measure(
                            () -> reservationService.heartbeatClaim(
                                    placeImport.getId(), claimToken, clock.instant()
                            ),
                            timing::addDbWrite
                    );
                    if (!heartbeatSucceeded) {
                        return;
                    }
                } catch (RuntimeException exception) {
                    logger.error("Place import processing failed: importId={}", placeImport.getId(), exception);
                    measureDbWrite(
                            () -> reservationService.failClaimed(
                                    placeImport.getId(), claimToken,
                                    ErrorCode.INTERNAL_ERROR.getCode(), clock.instant()
                            ),
                            timing
                    );
                    return;
                }
            }
        } finally {
            logTiming(placeImport, queueWaitMillis, totalStartedAt, timing);
        }
    }

    private boolean canRetryImmediately(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.PLACE_ANALYSIS_UNAVAILABLE
                || exception.getErrorCode() == ErrorCode.PLACE_VERIFICATION_UNAVAILABLE;
    }

    private void process(
            PlaceImport placeImport,
            ContentSourceType sourceType,
            String claimToken,
            ProcessingTiming timing
    ) {
        ContentMetadata metadata = measure(
                () -> metadataService.fetch(placeImport.getCanonicalUrl(), sourceType),
                timing::addMetadata
        );
        if (sourceType.storesPublicContent()) {
            if (measure(
                    () -> resultWriter.reuseUnchangedContent(placeImport.getId(), claimToken, metadata),
                    timing::addDbWrite
            )) {
                dispatchContentImageEnrichment(placeImport.getId(), metadata.imageUrls());
                dispatchPlaceImageEnrichment(placeImport.getId());
                return;
            }
            placeImport.attachContent(measure(
                    () -> resultWriter.saveMetadata(placeImport.getId(), claimToken, metadata),
                    timing::addDbWrite
            ));
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
        List<ExtractedPlace> extracted = isDirectPlaceSource(sourceType)
                ? List.of(new ExtractedPlace(metadata.title(), metadata.caption(),
                metadata.title() + " " + metadata.caption(), "EXPLICIT_VENUE"))
                : loadCachedCandidates(placeImport, metadata, model, prompt);
        if (extracted == null) {
            extracted = analyzeWithGemini(
                    placeImport, metadata, sourceText, model, prompt, claimToken, timing
            );
            if (extracted == null) {
                return;
            }
        }
        List<ExtractedPlace> extractedCandidates = extracted;
        if (isDirectPlaceSource(sourceType)) {
            measureDbWrite(
                    () -> resultWriter.saveExtractedCandidates(
                            placeImport.getId(), claimToken, extractedCandidates
                    ),
                    timing
            );
        }
        List<VerifiedCandidate> candidates = measure(
                () -> verifyCandidates(extractedCandidates),
                timing::addKakaoVerification
        );
        if (candidates.isEmpty()) {
            measureDbWrite(
                    () -> reservationService.failClaimed(
                            placeImport.getId(), claimToken,
                            ErrorCode.PLACE_NOT_VERIFIED.getCode(), clock.instant()
                    ),
                    timing
            );
            return;
        }
        measureDbWrite(
                () -> resultWriter.saveSuccess(placeImport.getId(), claimToken, metadata, candidates),
                timing
        );
        contentImageEnrichmentService.dispatch(placeImport.getContentId(), metadata.imageUrls());
        dispatchPlaceImageEnrichment(placeImport.getId());
    }

    private List<VerifiedCandidate> verifyCandidates(List<ExtractedPlace> extracted) {
        List<CompletableFuture<PlaceVerificationResult>> futures = new ArrayList<>();
        try {
            for (ExtractedPlace place : extracted) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> verifyCachedOrExternal(place), verificationExecutor
                ));
            }
        } catch (RejectedExecutionException exception) {
            waitForAllVerifications(futures);
            throw new PlaceVerificationUnavailableException(exception);
        }
        waitForAllVerifications(futures);
        List<VerifiedCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < extracted.size(); index++) {
            PlaceVerificationResult verification = awaitVerification(futures.get(index));
            if (verification != null) {
                ExtractedPlace extractedPlace = extracted.get(index);
                candidates.add(new VerifiedCandidate(
                        extractedPlace,
                        verification.place(),
                        verification.status()
                ));
            }
        }
        return candidates;
    }

    private void waitForAllVerifications(List<CompletableFuture<PlaceVerificationResult>> futures) {
        try {
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (CompletionException ignored) {
            // Preserve the original exception while ensuring sibling tasks have finished.
        }
    }

    private PlaceVerificationResult awaitVerification(
            CompletableFuture<PlaceVerificationResult> future
    ) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }

    private void dispatchContentImageEnrichment(Long importId, List<String> sourceUrls) {
        importRepository.findById(importId)
                .map(PlaceImport::getContentId)
                .ifPresent(contentId -> contentImageEnrichmentService.dispatch(contentId, sourceUrls));
    }

    private void dispatchPlaceImageEnrichment(Long importId) {
        if (imageEnrichmentDispatcher != null) {
            imageEnrichmentDispatcher.dispatchImport(importId);
            return;
        }
        if (imageEnrichmentService != null) {
            imageEnrichmentService.enrichImportPlaces(importId);
        }
    }

    private List<ExtractedPlace> analyzeWithGemini(PlaceImport placeImport, ContentMetadata metadata,
                                                    String sourceText, String model, String prompt,
                                                    String claimToken, ProcessingTiming timing) {
        Long contentId = placeImport.getContentId();
        if (contentId == null) {
            List<ExtractedPlace> extracted = measure(
                    () -> analyze(metadata, sourceText),
                    timing::addGemini
            );
            measureDbWrite(
                    () -> resultWriter.saveExtractedCandidates(placeImport.getId(), claimToken, extracted),
                    timing
            );
            return extracted;
        }
        String analysisClaim = measure(
                () -> resultWriter.claimAnalysis(
                        contentId,
                        metadata.contentHash(),
                        model,
                        prompt,
                        clock.instant(),
                        clock.instant().minusSeconds(properties.staleTimeoutSeconds())
                ),
                timing::addDbWrite
        );
        if (analysisClaim == null) {
            List<ExtractedPlace> cached = loadCachedCandidates(placeImport, metadata, model, prompt);
            if (cached == null) {
                measureDbWrite(
                        () -> reservationService.requeueClaimed(placeImport.getId(), claimToken, clock.instant()),
                        timing
                );
            }
            return cached;
        }
        try {
            List<ExtractedPlace> extracted = measure(
                    () -> analyze(metadata, sourceText),
                    timing::addGemini
            );
            boolean saved = measure(
                    () -> resultWriter.saveAnalysis(
                            contentId,
                            analysisClaim,
                            metadata.contentHash(),
                            model,
                            prompt,
                            extracted,
                            clock.instant()
                    ),
                    timing::addDbWrite
            );
            if (!saved) {
                measureDbWrite(
                        () -> reservationService.requeueClaimed(placeImport.getId(), claimToken, clock.instant()),
                        timing
                );
                return null;
            }
            measureDbWrite(
                    () -> resultWriter.saveExtractedCandidates(placeImport.getId(), claimToken, extracted),
                    timing
            );
            return extracted;
        } catch (RuntimeException exception) {
            measureDbWrite(() -> resultWriter.failAnalysis(contentId, analysisClaim), timing);
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

    private boolean isDirectPlaceSource(ContentSourceType sourceType) {
        return sourceType == ContentSourceType.NAVER_SHORT_LINK
                || sourceType == ContentSourceType.NAVER_MAP
                || sourceType == ContentSourceType.KAKAO_MAP;
    }

    private void measureDbWrite(Runnable action, ProcessingTiming timing) {
        measure(action, timing::addDbWrite);
    }

    private <T> T measure(Supplier<T> action, LongConsumer recorder) {
        long startedAt = System.nanoTime();
        try {
            return action.get();
        } finally {
            recorder.accept(elapsedMillis(startedAt));
        }
    }

    private void measure(Runnable action, LongConsumer recorder) {
        long startedAt = System.nanoTime();
        try {
            action.run();
        } finally {
            recorder.accept(elapsedMillis(startedAt));
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void logTiming(
            PlaceImport placeImport,
            long queueWaitMillis,
            long totalStartedAt,
            ProcessingTiming timing
    ) {
        logger.info(
                "place_import_timing importId={} metadata_ms={} gemini_ms={} "
                        + "kakao_verification_ms={} db_write_ms={} queue_wait_ms={} total_ms={}",
                placeImport.getId(),
                timing.metadataMillis,
                timing.geminiMillis,
                timing.kakaoVerificationMillis,
                timing.dbWriteMillis,
                queueWaitMillis,
                queueWaitMillis + elapsedMillis(totalStartedAt)
        );
    }

    private long queueWaitMillis(Instant queuedAt) {
        return Math.max(Duration.between(queuedAt, clock.instant()).toMillis(), 0L);
    }

    private static final class ProcessingTiming {

        private long metadataMillis;
        private long geminiMillis;
        private long kakaoVerificationMillis;
        private long dbWriteMillis;

        private void addMetadata(long elapsedMillis) {
            metadataMillis += elapsedMillis;
        }

        private void addGemini(long elapsedMillis) {
            geminiMillis += elapsedMillis;
        }

        private void addKakaoVerification(long elapsedMillis) {
            kakaoVerificationMillis += elapsedMillis;
        }

        private void addDbWrite(long elapsedMillis) {
            dbWriteMillis += elapsedMillis;
        }
    }
}
