package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PlaceImportViewMapper {

    static final long PROCESSING_RETRY_AFTER_SECONDS = 2L;

    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final MemberPlaceRepository memberPlaceRepository;
    private final PlaceAnalysisProperties properties;

    public PlaceImportViewMapper(
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            MemberPlaceRepository memberPlaceRepository,
            PlaceAnalysisProperties properties
    ) {
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.memberPlaceRepository = memberPlaceRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PlaceImportView toView(PlaceImport placeImport) {
        var storedCandidates = candidateRepository
                .findAllByImportIdOrderByIdAsc(placeImport.getId());
        var placeIds = storedCandidates.stream()
                .map(PlaceCandidate::getPlaceId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Place> places = findPlaces(placeIds);
        Set<Long> savedPlaceIds = findSavedPlaceIds(placeImport.getMemberId(), placeIds);
        var candidates = storedCandidates.stream()
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

    private Map<Long, Place> findPlaces(java.util.List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
    }

    private Set<Long> findSavedPlaceIds(Long memberId, java.util.List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Set.of();
        }
        return memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(memberId, placeIds)
                .stream()
                .map(MemberPlace::getPlace)
                .map(Place::getId)
                .collect(Collectors.toSet());
    }

    private PlaceCandidateView toCandidateView(
            PlaceCandidate candidate,
            Map<Long, Place> places,
            Set<Long> savedPlaceIds
    ) {
        Place place = candidate.getPlaceId() == null
                ? null
                : places.get(candidate.getPlaceId());
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
                place.getThumbnailUrl(),
                place.getImageUrls()
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
            return PROCESSING_RETRY_AFTER_SECONDS;
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
        String code = placeImport.getFailureCode();
        boolean retryable = ErrorCode.PLACE_METADATA_UNAVAILABLE.getCode().equals(code)
                || ErrorCode.PLACE_ANALYSIS_UNAVAILABLE.getCode().equals(code)
                || ErrorCode.PLACE_VERIFICATION_UNAVAILABLE.getCode().equals(code);
        return retryable && placeImport.getRetryCount() < properties.maxRetryCount();
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
