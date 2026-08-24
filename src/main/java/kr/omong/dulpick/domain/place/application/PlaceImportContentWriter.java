package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PlaceImportClaimLostException;
import kr.omong.dulpick.domain.place.application.ContentMetadata;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSubmissionRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaceImportContentWriter {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportContentWriter.class);

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final ContentRepository contentRepository;
    private final ContentPlaceRepository contentPlaceRepository;
    private final ContentSubmissionRepository submissionRepository;
    private final ContentImageStorageService imageStorageService;
    private final Clock clock;

    public PlaceImportContentWriter(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            ContentRepository contentRepository,
            ContentPlaceRepository contentPlaceRepository,
            ContentSubmissionRepository submissionRepository,
            ContentImageStorageService imageStorageService,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.contentRepository = contentRepository;
        this.contentPlaceRepository = contentPlaceRepository;
        this.submissionRepository = submissionRepository;
        this.imageStorageService = imageStorageService;
        this.clock = clock;
    }

    @Transactional
    public Long saveMetadata(Long importId, String claimToken, ContentMetadata metadata) {
        PlaceImport placeImport = requireClaim(importId, claimToken);
        Content content = findOrCreateContent(metadata);
        placeImport.attachContent(content.getId());
        submissionRepository.insertIfAbsent(content.getId(), placeImport.getMemberId(), clock.instant());
        placeImport.recordMetadata(displayTitle(metadata), metadata.caption(), metadata.thumbnailUrl(),
                metadata.contentHash(), metadata.sourceUpdatedAt());
        imageStorageService.storeIfAvailable(content, metadata.imageUrls());
        recordSourceMetadata(placeImport, metadata);
        return content.getId();
    }

    @Transactional
    public boolean reuseUnchangedContent(Long importId, String claimToken, ContentMetadata metadata) {
        requireClaim(importId, claimToken);
        if (!metadata.sourceType().storesPublicContent()) {
            return false;
        }
        String urlHash = Sha256.hex(metadata.canonicalUrl());
        Content content = contentRepository.findByCanonicalUrlHash(urlHash).orElse(null);
        if (content == null || !metadata.contentHash().equals(content.getContentHash())) {
            return false;
        }
        List<Place> places = contentPlaceRepository.findAllByContentId(content.getId()).stream()
                .map(ContentPlace::getPlaceId)
                .map(placeRepository::findById)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (places.isEmpty()) {
            return false;
        }
        saveMetadata(importId, claimToken, metadata);
        candidateRepository.deleteAllByImportId(importId);
        candidateRepository.saveAll(places.stream()
                .map(place -> PlaceCandidate.verified(importId, place.getId(), place.getName(),
                        place.getAddress(), null, null, clock.instant()))
                .toList());
        requireClaim(importId, claimToken).complete(displayTitle(metadata), metadata.caption(),
                metadata.thumbnailUrl(), metadata.contentHash(), metadata.sourceUpdatedAt(), clock.instant());
        return true;
    }

    @Transactional
    public void saveSuccess(Long importId, String claimToken, ContentMetadata metadata,
                            List<VerifiedCandidate> verifiedCandidates) {
        List<VerifiedCandidate> uniqueCandidates = uniqueCandidates(verifiedCandidates);
        PlaceImport placeImport = requireClaim(importId, claimToken);
        Long contentId = placeImport.getContentId();
        if (contentId == null && metadata.sourceType().storesPublicContent()) {
            contentId = findOrCreateContent(metadata).getId();
            placeImport.attachContent(contentId);
        }
        final Long resolvedContentId = contentId;
        candidateRepository.deleteAllByImportId(importId);
        if (resolvedContentId != null) {
            contentPlaceRepository.deleteAllByContentId(resolvedContentId);
        }
        List<PlaceCandidate> candidates = uniqueCandidates.stream()
                .map(candidate -> saveCandidate(importId, resolvedContentId, candidate))
                .toList();
        candidateRepository.saveAll(candidates);
        if (resolvedContentId != null) {
            contentRepository.findById(resolvedContentId).ifPresent(content -> content.updateMetadata(
                    displayTitle(metadata), metadata.caption(), metadata.thumbnailUrl(),
                    metadata.contentHash(), clock.instant()));
            contentRepository.findById(resolvedContentId).ifPresent(content ->
                    imageStorageService.storeIfAvailable(content, metadata.imageUrls())
            );
            contentRepository.findById(resolvedContentId).ifPresent(content -> content.updateSourceMetadata(
                    metadata.sourceAuthorName(), metadata.sourceAuthorUsername(), metadata.sourcePublishedOn(),
                    metadata.likeCount(), metadata.commentCount(), metadata.engagementCheckedAt()));
            candidates.forEach(candidate -> contentPlaceRepository.insertIfAbsent(
                    resolvedContentId, candidate.getPlaceId(), clock.instant()));
            contentRepository.findById(resolvedContentId)
                    .ifPresent(content -> content.updatePlaceCount(candidates.size()));
            contentRepository.findById(resolvedContentId)
                    .ifPresent(content -> content.publish(clock.instant()));
        }
        placeImport.complete(displayTitle(metadata), metadata.caption(), metadata.thumbnailUrl(),
                metadata.contentHash(), metadata.sourceUpdatedAt(), clock.instant());
        recordSourceMetadata(placeImport, metadata);
    }

    private PlaceImport requireClaim(Long importId, String claimToken) {
        return importRepository.findClaimedForUpdate(importId, claimToken)
                .orElseThrow(PlaceImportClaimLostException::new);
    }

    private List<VerifiedCandidate> uniqueCandidates(List<VerifiedCandidate> candidates) {
        Map<String, VerifiedCandidate> unique = new LinkedHashMap<>();
        candidates.forEach(candidate -> unique.merge(candidate.verified().kakaoPlaceId(), candidate,
                this::preferVerified));
        return unique.values().stream().toList();
    }

    private VerifiedCandidate preferVerified(VerifiedCandidate first, VerifiedCandidate second) {
        return first.verificationStatus() == PlaceVerificationStatus.VERIFIED ? first : second;
    }

    private PlaceCandidate saveCandidate(Long importId, Long contentId, VerifiedCandidate candidate) {
        VerifiedPlace verified = candidate.verified();
        Instant now = clock.instant();
        logFallbackCategory(verified);
        placeRepository.insertIfAbsent(verified.kakaoPlaceId(), verified.name(), verified.address(),
                verified.roadAddress(), verified.latitude(), verified.longitude(), verified.category(),
                verified.categoryGroupCode(), verified.phone(), verified.kakaoPlaceUrl(),
                verified.thumbnailUrl(), now);
        Place place = placeRepository.findByKakaoPlaceId(verified.kakaoPlaceId())
                .orElseThrow(IllegalStateException::new);
        return PlaceCandidate.matched(importId, place.getId(), candidate.extracted().name(),
                candidate.extracted().addressHint(), candidate.extracted().evidence(),
                candidate.extracted().mentionType(), candidate.verificationStatus(), now);
    }

    private void logFallbackCategory(VerifiedPlace verified) {
        if (DulpickPlaceCategory.isFallback(verified.categoryGroupCode(), verified.category())) {
            logger.warn(
                    "place_category_fallback source=IMPORT kakaoPlaceId={} categoryGroupCode={} category={}",
                    verified.kakaoPlaceId(),
                    verified.categoryGroupCode(),
                    verified.category()
            );
        }
    }

    private Content findOrCreateContent(ContentMetadata metadata) {
        String urlHash = Sha256.hex(metadata.canonicalUrl());
        contentRepository.insertIfAbsent(metadata.canonicalUrl(), urlHash, metadata.sourceType().name(),
                displayTitle(metadata), metadata.caption(), metadata.thumbnailUrl(), metadata.contentHash(),
                clock.instant());
        return contentRepository.findByCanonicalUrlHash(urlHash)
                .orElseThrow(IllegalStateException::new);
    }

    private String displayTitle(ContentMetadata metadata) {
        String title = metadata.title() == null ? "" : metadata.title().strip();
        if (metadata.sourceType().name().startsWith("INSTAGRAM")) {
            int separator = title.indexOf(": ");
            if (separator >= 0 && separator + 2 < title.length()) {
                title = title.substring(separator + 2).strip();
            }
        }
        String firstLine = title.split("\\R", 2)[0].strip();
        if (firstLine.isBlank()) {
            firstLine = (metadata.caption() == null ? "" : metadata.caption()).strip();
        }
        firstLine = firstLine.replaceAll("^[\\\"'‘’“”]+|[\\\"'‘’“”]+$", "").strip();
        return firstLine.length() > 200 ? firstLine.substring(0, 200).strip() : firstLine;
    }

    private void recordSourceMetadata(PlaceImport placeImport, ContentMetadata metadata) {
        placeImport.recordSourceMetadata(metadata.sourceAuthorName(), metadata.sourceAuthorUsername(),
                metadata.sourcePublishedOn(), metadata.likeCount(), metadata.commentCount(),
                metadata.engagementCheckedAt());
    }
}
