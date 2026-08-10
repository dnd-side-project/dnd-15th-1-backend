package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.place.application.exception.PlaceAnalysisUnavailableException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PlaceImportService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportService.class);

    private final MemberRepository memberRepository;
    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceImportResultWriter resultWriter;
    private final PlaceImportViewMapper viewMapper;
    private final PlaceImageEnrichmentService imageEnrichmentService;
    private final PlaceImportReservationService reservationService;
    private final ContentSourceUrlParser urlParser;
    private final MetadataService metadataService;
    private final PlaceAnalyzer placeAnalyzer;
    private final PlaceVerifier placeVerifier;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;

    public PlaceImportService(
            MemberRepository memberRepository,
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceImportResultWriter resultWriter,
            PlaceImportViewMapper viewMapper,
            PlaceImageEnrichmentService imageEnrichmentService,
            PlaceImportReservationService reservationService,
            ContentSourceUrlParser urlParser,
            MetadataService metadataService,
            PlaceAnalyzer placeAnalyzer,
            PlaceVerifier placeVerifier,
            PlaceAnalysisProperties properties,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.resultWriter = resultWriter;
        this.viewMapper = viewMapper;
        this.imageEnrichmentService = imageEnrichmentService;
        this.reservationService = reservationService;
        this.urlParser = urlParser;
        this.metadataService = metadataService;
        this.placeAnalyzer = placeAnalyzer;
        this.placeVerifier = placeVerifier;
        this.properties = properties;
        this.clock = clock;
    }

    public PlaceImportSubmissionView importLink(Long memberId, String rawUrl) {
        if (!properties.enabled()) {
            throw new PlaceAnalysisUnavailableException(null);
        }
        Member member = findActiveMember(memberId);
        ContentSourceUrlParser.ParsedSource source = urlParser.parse(rawUrl);
        String urlHash = Sha256.hex(source.canonicalUrl());
        PlaceImport existing = importRepository
                .findByMemberIdAndCanonicalUrlHash(memberId, urlHash)
                .orElse(null);
        if (existing != null) {
            if (canRetry(existing)
                    && reservationService.requeueRetryable(
                    existing.getId(),
                    clock.instant(),
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds()),
                    clock.instant().minusSeconds(properties.retryCooldownSeconds()))) {
                existing = reload(existing);
            }
            return new PlaceImportSubmissionView(viewMapper.toView(existing));
        }
        Instant now = clock.instant();
        PlaceImportReservationService.Reservation reservation = reservationService.reserve(
                member.getId(),
                source.canonicalUrl(),
                urlHash,
                source.sourceType(),
                now
        );
        PlaceImport placeImport = importRepository.findById(reservation.importId())
                .orElseThrow(IllegalStateException::new);
        return new PlaceImportSubmissionView(viewMapper.toView(placeImport));
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

    private void processWithRetry(
            PlaceImport placeImport,
            ContentSourceType sourceType,
            String importClaimToken
    ) {
        int attempts = properties.maxRetries() + 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                process(placeImport, sourceType, importClaimToken);
                return;
            } catch (PlaceImportClaimLostException exception) {
                logger.info("Place import claim lost: importId={}", placeImport.getId());
                return;
            } catch (BusinessException exception) {
                if (!canRetryImmediately(exception) || attempt == attempts - 1) {
                    reservationService.failClaimed(
                            placeImport.getId(),
                            importClaimToken,
                            exception.getErrorCode().getCode(),
                            clock.instant()
                    );
                    return;
                }
                if (!reservationService.heartbeatClaim(
                        placeImport.getId(),
                        importClaimToken,
                        clock.instant()
                )) {
                    return;
                }
            } catch (RuntimeException exception) {
                logger.error("Place import processing failed: importId={}", placeImport.getId(), exception);
                reservationService.failClaimed(
                        placeImport.getId(),
                        importClaimToken,
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        clock.instant()
                );
                return;
            }
        }
    }

    private boolean canRetry(PlaceImport placeImport) {
        if (placeImport.getStatus() == PlaceImportStatus.FAILED
                || placeImport.getStatus() == PlaceImportStatus.RECEIVED) {
            if (placeImport.getStatus() == PlaceImportStatus.RECEIVED) {
                return true;
            }
            return placeImport.getRetryCount() < properties.maxRetryCount()
                    && placeImport.getUpdatedAt()
                    .plusSeconds(properties.retryCooldownSeconds())
                    .isBefore(clock.instant());
        }
        if (placeImport.getStatus() != PlaceImportStatus.PROCESSING) {
            return false;
        }
        return placeImport.getUpdatedAt().plusSeconds(properties.staleTimeoutSeconds())
                .isBefore(clock.instant());
    }

    private boolean canRetryImmediately(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.PLACE_ANALYSIS_UNAVAILABLE
                || exception.getErrorCode() == ErrorCode.PLACE_VERIFICATION_UNAVAILABLE;
    }

    private ExtractedPlace validateEvidence(ExtractedPlace candidate, String sourceText) {
        if (candidate.evidence() == null || candidate.evidence().isBlank()) {
            return candidate;
        }
        if (sourceText.contains(normalizeText(candidate.evidence()))) {
            return candidate;
        }
        return new ExtractedPlace(
                candidate.name(),
                candidate.addressHint(),
                null,
                candidate.mentionType()
        );
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("\\s+", "");
    }

    private void process(
            PlaceImport placeImport,
            ContentSourceType sourceType,
            String importClaimToken
    ) {
        ContentMetadata metadata = metadataService.fetch(
                placeImport.getCanonicalUrl(),
                sourceType
        );
        if (sourceType.storesPublicContent()) {
            if (resultWriter.reuseUnchangedContent(
                    placeImport.getId(),
                    importClaimToken,
                    metadata
            )) {
                imageEnrichmentService.enrichImportPlaces(placeImport.getId());
                return;
            }
            Long contentId = resultWriter.saveMetadata(
                    placeImport.getId(),
                    importClaimToken,
                    metadata
            );
            placeImport.attachContent(contentId);
        } else {
            placeImport.recordMetadata(
                    metadata.title(),
                    metadata.caption(),
                    metadata.thumbnailUrl(),
                    metadata.contentHash(),
                    metadata.sourceUpdatedAt()
            );
            placeImport.recordSourceMetadata(
                    metadata.sourceAuthorName(),
                    metadata.sourceAuthorUsername(),
                    metadata.sourcePublishedOn(),
                    metadata.likeCount(),
                    metadata.commentCount(),
                    metadata.engagementCheckedAt()
            );
        }
        String sourceText = normalizeText(metadata.title() + " " + metadata.caption());
        String analyzerModel = placeAnalyzer.modelKey();
        String promptVersion = placeAnalyzer.promptVersion();
        List<ExtractedPlace> extractedPlaces = isNaverPlace(sourceType)
                ? null
                : loadCachedCandidates(
                        placeImport,
                        metadata,
                        analyzerModel,
                        promptVersion
                );
        if (extractedPlaces == null) {
            if (isNaverPlace(sourceType)) {
                extractedPlaces = List.of(new ExtractedPlace(
                        metadata.title(),
                        metadata.caption(),
                        metadata.title() + " " + metadata.caption(),
                        "EXPLICIT_VENUE"
                ));
                resultWriter.saveExtractedCandidates(
                        placeImport.getId(),
                        importClaimToken,
                        extractedPlaces
                );
            } else {
                extractedPlaces = analyzeWithGemini(
                        placeImport,
                        metadata,
                        sourceText,
                        analyzerModel,
                        promptVersion,
                        importClaimToken
                );
                if (extractedPlaces == null) {
                    return;
                }
            }
        }
        List<VerifiedCandidate> candidates = new ArrayList<>();
        for (ExtractedPlace extractedPlace : extractedPlaces) {
            PlaceVerificationResult verification = placeVerifier.verify(extractedPlace);
            if (verification == null) {
                continue;
            }
            candidates.add(new VerifiedCandidate(
                    extractedPlace,
                    verification.place(),
                    verification.status()
            ));
        }
        if (candidates.isEmpty()) {
            reservationService.failClaimed(
                    placeImport.getId(),
                    importClaimToken,
                    ErrorCode.PLACE_NOT_VERIFIED.getCode(),
                    clock.instant()
            );
            return;
        }
        resultWriter.saveSuccess(
                placeImport.getId(),
                importClaimToken,
                metadata,
                candidates
        );
        imageEnrichmentService.enrichImportPlaces(placeImport.getId());
    }

    private List<ExtractedPlace> analyzeWithGemini(
            PlaceImport placeImport,
            ContentMetadata metadata,
            String sourceText,
            String analyzerModel,
            String promptVersion,
            String importClaimToken
    ) {
        List<ExtractedPlace> extractedPlaces;
        Long contentId = placeImport.getContentId();
        if (contentId != null) {
            String analysisClaimToken = resultWriter.claimAnalysis(
                    contentId,
                    metadata.contentHash(),
                    analyzerModel,
                    promptVersion,
                    clock.instant(),
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds())
            );
            if (analysisClaimToken == null) {
                extractedPlaces = loadCachedCandidates(
                        placeImport,
                        metadata,
                        analyzerModel,
                        promptVersion
                );
                if (extractedPlaces == null) {
                    reservationService.requeueClaimed(
                            placeImport.getId(),
                            importClaimToken,
                            clock.instant()
                    );
                    return null;
                }
            } else {
                try {
                    extractedPlaces = placeAnalyzer.analyze(metadata).stream()
                            .map(candidate -> validateEvidence(candidate, sourceText))
                            .limit(properties.maxCandidates())
                            .toList();
                    boolean saved = resultWriter.saveAnalysis(
                            contentId,
                            analysisClaimToken,
                            metadata.contentHash(),
                            analyzerModel,
                            promptVersion,
                            extractedPlaces,
                            clock.instant()
                    );
                    if (!saved) {
                        reservationService.requeueClaimed(
                                placeImport.getId(),
                                importClaimToken,
                                clock.instant()
                        );
                        return null;
                    }
                    resultWriter.saveExtractedCandidates(
                            placeImport.getId(),
                            importClaimToken,
                            extractedPlaces
                    );
                } catch (RuntimeException exception) {
                    resultWriter.failAnalysis(contentId, analysisClaimToken);
                    throw exception;
                }
            }
        } else {
            extractedPlaces = placeAnalyzer.analyze(metadata).stream()
                        .map(candidate -> validateEvidence(candidate, sourceText))
                        .limit(properties.maxCandidates())
                        .toList();
            resultWriter.saveExtractedCandidates(
                    placeImport.getId(),
                    importClaimToken,
                    extractedPlaces
            );
        }
        return extractedPlaces;
    }

    private boolean isNaverPlace(ContentSourceType sourceType) {
        return sourceType == ContentSourceType.NAVER_SHORT_LINK
                || sourceType == ContentSourceType.NAVER_MAP;
    }

    private List<ExtractedPlace> loadCachedCandidates(
            PlaceImport placeImport,
            ContentMetadata metadata,
            String analyzerModel,
            String promptVersion
    ) {
        if (placeImport.getContentId() != null) {
            return resultWriter.loadCachedAnalysis(
                            placeImport.getContentId(),
                            metadata.contentHash(),
                            analyzerModel,
                            promptVersion
                    )
                    .orElse(null);
        }
        List<ExtractedPlace> candidates = candidateRepository
                .findAllByImportIdOrderByIdAsc(placeImport.getId())
                .stream()
                .filter(candidate -> candidate.getVerificationStatus() == PlaceVerificationStatus.EXTRACTED)
                .map(candidate -> new ExtractedPlace(
                        candidate.getExtractedName(),
                        candidate.getExtractedAddressHint(),
                        candidate.getEvidence(),
                        candidate.getMentionType()
                ))
                .toList();
        return candidates.isEmpty() ? null : candidates;
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private PlaceImport reload(PlaceImport placeImport) {
        return importRepository.findById(placeImport.getId()).orElse(placeImport);
    }
}
