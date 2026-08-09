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
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
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

@Service
public class PlaceImportService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportService.class);

    private final MemberRepository memberRepository;
    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
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
        this.resultWriter = resultWriter;
        this.reservationService = reservationService;
        this.urlParser = urlParser;
        this.metadataService = metadataService;
        this.placeAnalyzer = placeAnalyzer;
        this.placeVerifier = placeVerifier;
        this.properties = properties;
        this.clock = clock;
    }

    public PlaceImportView importLink(Long memberId, String rawUrl) {
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
            if (isContentChanged(existing, source)) {
                Instant now = clock.instant();
                if (reservationService.claimChangedCompleted(existing.getId(), now)) {
                    processWithRetry(reload(existing), source.sourceType());
                }
                return toView(reload(existing));
            }
            if (canRetry(existing)
                    && reservationService.claimRetryable(
                    existing.getId(),
                    clock.instant(),
                    clock.instant().minusSeconds(properties.staleTimeoutSeconds()))) {
                processWithRetry(reload(existing), source.sourceType());
            }
            return toView(reload(existing));
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
        if (!reservation.claimed()) {
            return toView(reload(placeImport));
        }
        processWithRetry(placeImport, source.sourceType());
        return toView(reload(placeImport));
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
                if (!isRetryable(exception) || attempt == attempts - 1) {
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
            return true;
        }
        if (placeImport.getStatus() != PlaceImportStatus.PROCESSING) {
            return false;
        }
        return placeImport.getUpdatedAt().plusSeconds(properties.staleTimeoutSeconds())
                .isBefore(clock.instant());
    }

    private boolean isContentChanged(
            PlaceImport existing,
            ContentSourceUrlParser.ParsedSource source
    ) {
        if (existing.getContentHash() == null
                || existing.getStatus() == PlaceImportStatus.PROCESSING
                || existing.getStatus() == PlaceImportStatus.FAILED) {
            return false;
        }
        try {
            ContentMetadata metadata = metadataService.fetch(
                    source.canonicalUrl(),
                    source.sourceType()
            );
            return !metadata.contentHash().equals(existing.getContentHash());
        } catch (BusinessException exception) {
            return false;
        }
    }

    private boolean isRetryable(BusinessException exception) {
        return exception.getErrorCode() == ErrorCode.PLACE_METADATA_UNAVAILABLE
                || exception.getErrorCode() == ErrorCode.PLACE_ANALYSIS_UNAVAILABLE
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
        }
        String sourceText = normalizeText(metadata.title() + " " + metadata.caption());
        List<ExtractedPlace> extractedPlaces = placeAnalyzer.analyze(metadata).stream()
                .map(candidate -> validateEvidence(candidate, sourceText))
                .limit(properties.maxCandidates())
                .toList();
        List<VerifiedCandidate> candidates = new ArrayList<>();
        for (ExtractedPlace extractedPlace : extractedPlaces) {
            VerifiedPlace verifiedPlace = placeVerifier.verify(extractedPlace);
            if (verifiedPlace == null) {
                continue;
            }
            candidates.add(new VerifiedCandidate(extractedPlace, verifiedPlace));
        }
        if (candidates.isEmpty()) {
            placeImport.fail(ErrorCode.PLACE_NOT_VERIFIED.getCode(), clock.instant());
            importRepository.save(placeImport);
            return;
        }
        resultWriter.saveSuccess(placeImport.getId(), metadata, candidates);
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
        List<PlaceCandidateView> candidates = candidateRepository
                .findAllByImportIdOrderByIdAsc(placeImport.getId())
                .stream()
                .map(this::toCandidateView)
                .toList();
        return new PlaceImportView(
                placeImport.getId(),
                placeImport.getContentId(),
                placeImport.getCanonicalUrl(),
                placeImport.getSourceType(),
                placeImport.getTitle(),
                placeImport.getContent(),
                placeImport.getThumbnailUrl(),
                placeImport.getStatus(),
                placeImport.getFailureCode(),
                candidates
        );
    }

    private PlaceImport reload(PlaceImport placeImport) {
        return importRepository.findById(placeImport.getId()).orElse(placeImport);
    }

    private PlaceCandidateView toCandidateView(PlaceCandidate candidate) {
        Place place = placeRepository.findById(candidate.getPlaceId()).orElse(null);
        if (place == null) {
            return new PlaceCandidateView(
                    candidate.getId(),
                    null,
                    candidate.getExtractedName(),
                    null,
                    null,
                    null,
                    null,
                    candidate.getEvidence(),
                    candidate.getMentionType()
            );
        }
        return new PlaceCandidateView(
                candidate.getId(),
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getKakaoPlaceId(),
                place.getCategory(),
                candidate.getEvidence(),
                candidate.getMentionType()
        );
    }
}
