package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportAccessDeniedException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportNotFoundException;
import kr.omong.dulpick.domain.place.application.exception.PlaceAnalysisUnavailableException;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlaceImportService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportService.class);

    private final MemberRepository memberRepository;
    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final MemberPlaceRepository memberPlaceRepository;
    private final PlaceImportResultWriter resultWriter;
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
            PlaceRepository placeRepository,
            MemberPlaceRepository memberPlaceRepository,
            PlaceImportResultWriter resultWriter,
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
        this.placeRepository = placeRepository;
        this.memberPlaceRepository = memberPlaceRepository;
        this.resultWriter = resultWriter;
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
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds()))) {
                existing = reload(existing);
            }
            return new PlaceImportSubmissionView(toView(existing));
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
        return new PlaceImportSubmissionView(toView(placeImport));
    }

    public boolean claimPending(Long importId) {
        PlaceImport placeImport = importRepository.findById(importId).orElse(null);
        if (!isRecoverable(placeImport)) {
            return false;
        }
        Instant now = clock.instant();
        return reservationService.claimRetryable(
                importId,
                now,
                now.minusSeconds(properties.staleTimeoutSeconds())
        );
    }

    public void processClaimed(Long importId) {
        PlaceImport placeImport = importRepository.findById(importId).orElse(null);
        if (placeImport == null || placeImport.getStatus() != PlaceImportStatus.PROCESSING) {
            return;
        }
        processWithRetry(placeImport, placeImport.getSourceType());
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
            ContentSourceType sourceType
    ) {
        int attempts = properties.maxRetries() + 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                process(placeImport, sourceType);
                return;
            } catch (BusinessException exception) {
                if (!canRetryImmediately(exception) || attempt == attempts - 1) {
                    placeImport.fail(exception.getErrorCode().getCode(), clock.instant());
                    importRepository.save(placeImport);
                    return;
                }
                placeImport.retry(clock.instant());
                importRepository.save(placeImport);
            } catch (RuntimeException exception) {
                logger.error("Place import processing failed: importId={}", placeImport.getId(), exception);
                placeImport.fail(ErrorCode.INTERNAL_ERROR.getCode(), clock.instant());
                importRepository.save(placeImport);
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

    @Transactional(readOnly = true)
    public PlaceImportView get(Long memberId, Long importId) {
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(PlaceImportNotFoundException::new);
        if (!placeImport.getMemberId().equals(memberId)) {
            throw new PlaceImportAccessDeniedException();
        }
        return toView(placeImport);
    }

    private void process(PlaceImport placeImport, ContentSourceType sourceType) {
        ContentMetadata metadata = metadataService.fetch(
                placeImport.getCanonicalUrl(),
                sourceType
        );
        if (sourceType.storesPublicContent()) {
            if (resultWriter.reuseUnchangedContent(placeImport.getId(), metadata)) {
                return;
            }
            Long contentId = resultWriter.saveMetadata(placeImport.getId(), metadata);
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
                resultWriter.saveExtractedCandidates(placeImport.getId(), extractedPlaces);
            } else {
                extractedPlaces = analyzeWithGemini(
                        placeImport,
                        metadata,
                        sourceText,
                        analyzerModel,
                        promptVersion
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
            placeImport.fail(ErrorCode.PLACE_NOT_VERIFIED.getCode(), clock.instant());
            importRepository.save(placeImport);
            return;
        }
        resultWriter.saveSuccess(placeImport.getId(), metadata, candidates);
    }

    private List<ExtractedPlace> analyzeWithGemini(
            PlaceImport placeImport,
            ContentMetadata metadata,
            String sourceText,
            String analyzerModel,
            String promptVersion
    ) {
        List<ExtractedPlace> extractedPlaces;
        Long contentId = placeImport.getContentId();
        if (contentId != null) {
            String claimToken = resultWriter.claimAnalysis(
                    contentId,
                    metadata.contentHash(),
                    analyzerModel,
                    promptVersion,
                    clock.instant(),
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds())
            );
            if (claimToken == null) {
                extractedPlaces = loadCachedCandidates(
                        placeImport,
                        metadata,
                        analyzerModel,
                        promptVersion
                );
                if (extractedPlaces == null) {
                    placeImport.requeue(clock.instant());
                    importRepository.save(placeImport);
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
                            claimToken,
                            metadata.contentHash(),
                            analyzerModel,
                            promptVersion,
                            extractedPlaces,
                            clock.instant()
                    );
                    if (!saved) {
                        placeImport.requeue(clock.instant());
                        importRepository.save(placeImport);
                        return null;
                    }
                    resultWriter.saveExtractedCandidates(placeImport.getId(), extractedPlaces);
                } catch (RuntimeException exception) {
                    resultWriter.failAnalysis(contentId, claimToken);
                    throw exception;
                }
            }
        } else {
            extractedPlaces = placeAnalyzer.analyze(metadata).stream()
                        .map(candidate -> validateEvidence(candidate, sourceText))
                        .limit(properties.maxCandidates())
                        .toList();
            resultWriter.saveExtractedCandidates(placeImport.getId(), extractedPlaces);
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

    private PlaceImportView toView(PlaceImport placeImport) {
        List<PlaceCandidate> storedCandidates = candidateRepository
                .findAllByImportIdOrderByIdAsc(placeImport.getId());
        List<Long> placeIds = storedCandidates.stream()
                .map(PlaceCandidate::getPlaceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Place> places = findPlaces(placeIds);
        Set<Long> savedPlaceIds = findSavedPlaceIds(placeImport.getMemberId(), placeIds);
        List<PlaceCandidateView> candidates = storedCandidates.stream()
                .map(candidate -> toCandidateView(candidate, places, savedPlaceIds))
                .toList();
        return new PlaceImportView(
                placeImport.getId(),
                placeImport.getContentId(),
                placeImport.getCanonicalUrl(),
                placeImport.getSourceType(),
                placeImport.getStatus(),
                nextAction(placeImport),
                retryAfterSeconds(placeImport),
                failure(placeImport),
                content(placeImport),
                placeImport.getCreatedAt(),
                placeImport.getUpdatedAt(),
                candidates
        );
    }

    private Map<Long, Place> findPlaces(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
    }

    private Set<Long> findSavedPlaceIds(Long memberId, List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Set.of();
        }
        return memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(memberId, placeIds)
                .stream()
                .map(MemberPlace::getPlace)
                .map(Place::getId)
                .collect(Collectors.toSet());
    }

    private PlaceImport reload(PlaceImport placeImport) {
        return importRepository.findById(placeImport.getId()).orElse(placeImport);
    }

    private PlaceCandidateView toCandidateView(
            PlaceCandidate candidate,
            Map<Long, Place> places,
            Set<Long> savedPlaceIds
    ) {
        Long placeId = candidate.getPlaceId();
        Place place = placeId == null ? null : places.get(placeId);
        return new PlaceCandidateView(
                candidate.getId(),
                candidate.getVerificationStatus(),
                candidate.getExtractedName(),
                candidate.getExtractedAddressHint(),
                toVerifiedPlaceView(place, savedPlaceIds),
                candidate.getEvidence(),
                candidate.getMentionType()
        );
    }

    private PlaceCandidateView.VerifiedPlaceView toVerifiedPlaceView(
            Place place,
            Set<Long> savedPlaceIds
    ) {
        if (place == null) {
            return null;
        }
        return new PlaceCandidateView.VerifiedPlaceView(
                place.getId(),
                place.getKakaoPlaceId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                place.getCategoryName(),
                savedPlaceIds.contains(place.getId()),
                place.getThumbnailUrl()
        );
    }

    private PlaceImportNextAction nextAction(PlaceImport placeImport) {
        return switch (placeImport.getStatus()) {
            case RECEIVED, PROCESSING -> PlaceImportNextAction.WAIT;
            case REVIEW_REQUIRED -> PlaceImportNextAction.SELECT_PLACES;
            case COMPLETED -> PlaceImportNextAction.COMPLETED;
            case FAILED -> isRetryableFailure(placeImport)
                    ? PlaceImportNextAction.RETRY
                    : PlaceImportNextAction.NONE;
        };
    }

    private Long retryAfterSeconds(PlaceImport placeImport) {
        if (placeImport.getStatus() == PlaceImportStatus.RECEIVED
                || placeImport.getStatus() == PlaceImportStatus.PROCESSING) {
            return Math.max(properties.recoveryDelay().toSeconds(), 1L);
        }
        if (placeImport.getStatus() == PlaceImportStatus.FAILED
                && isRetryableFailure(placeImport)) {
            return (long) properties.retryCooldownSeconds();
        }
        return null;
    }

    private PlaceImportView.FailureView failure(PlaceImport placeImport) {
        if (placeImport.getFailureCode() == null) {
            return null;
        }
        return new PlaceImportView.FailureView(
                placeImport.getFailureCode(),
                isRetryableFailure(placeImport)
        );
    }

    private boolean isRetryableFailure(PlaceImport placeImport) {
        String failureCode = placeImport.getFailureCode();
        boolean retryableCode = ErrorCode.PLACE_METADATA_UNAVAILABLE.getCode().equals(failureCode)
                || ErrorCode.PLACE_ANALYSIS_UNAVAILABLE.getCode().equals(failureCode)
                || ErrorCode.PLACE_VERIFICATION_UNAVAILABLE.getCode().equals(failureCode);
        return retryableCode && placeImport.getRetryCount() < properties.maxRetryCount();
    }

    private PlaceImportView.ContentView content(PlaceImport placeImport) {
        return new PlaceImportView.ContentView(
                placeImport.getTitle(),
                placeImport.getContent(),
                placeImport.getThumbnailUrl(),
                author(placeImport),
                placeImport.getSourcePublishedOn(),
                engagement(placeImport)
        );
    }

    private PlaceImportView.AuthorView author(PlaceImport placeImport) {
        if (placeImport.getSourceAuthorName() == null
                && placeImport.getSourceAuthorUsername() == null) {
            return null;
        }
        return new PlaceImportView.AuthorView(
                placeImport.getSourceAuthorName(),
                placeImport.getSourceAuthorUsername()
        );
    }

    private PlaceImportView.EngagementView engagement(PlaceImport placeImport) {
        if (placeImport.getLikeCount() == null
                && placeImport.getCommentCount() == null
                && placeImport.getEngagementCheckedAt() == null) {
            return null;
        }
        return new PlaceImportView.EngagementView(
                placeImport.getLikeCount(),
                placeImport.getCommentCount(),
                placeImport.getEngagementCheckedAt()
        );
    }
}
