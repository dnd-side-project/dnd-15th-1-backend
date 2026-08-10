package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.application.event.ContentSavedEvent;
import kr.omong.dulpick.domain.place.application.exception.PlaceAlreadySavedException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportAccessDeniedException;
import kr.omong.dulpick.domain.place.application.exception.PlaceImportNotFoundException;
import kr.omong.dulpick.domain.place.application.exception.InvalidPlaceCandidateException;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImport;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PlaceCommandService {

    private final PlaceImportRepository importRepository;
    private final PlaceCandidateRepository candidateRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final MemberPlaceRepository memberPlaceRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public PlaceCommandService(
            PlaceImportRepository importRepository,
            PlaceCandidateRepository candidateRepository,
            PlaceRepository placeRepository,
            MemberRepository memberRepository,
            MemberPlaceRepository memberPlaceRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.candidateRepository = candidateRepository;
        this.placeRepository = placeRepository;
        this.memberRepository = memberRepository;
        this.memberPlaceRepository = memberPlaceRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MemberPlaceView saveManual(
            Long memberId,
            PlaceSearchResult searchResult,
            String alias,
            String memo
    ) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        placeRepository.insertIfAbsent(
                searchResult.kakaoPlaceId(),
                searchResult.name(),
                searchResult.address(),
                searchResult.roadAddress(),
                searchResult.latitude(),
                searchResult.longitude(),
                searchResult.category(),
                searchResult.categoryGroupCode(),
                searchResult.thumbnailUrl(),
                clock.instant()
        );
        Place place = placeRepository.findByKakaoPlaceId(searchResult.kakaoPlaceId())
                .orElseThrow(PlaceNotFoundException::new);
        ActiveCoupleMember membership = activeCoupleMemberRepository
                .findByMemberId(memberId)
                .orElse(null);
        Long partnerId = partnerId(membership, memberId);
        Instant now = clock.instant();
        MemberPlace saved = memberPlaceRepository.findByMemberIdAndPlaceId(memberId, place.getId())
                .orElseGet(() -> {
                    MemberPlace created = memberPlaceRepository.save(MemberPlace.save(
                            memberId,
                            place,
                            null,
                            alias,
                            memo,
                            now
                    ));
                    publishSavedEvent(membership, memberId, partnerId, place.getId(), now);
                    return created;
                });
        return toView(
                saved,
                place,
                ownershipStatus(partnerId, place.getId())
        );
    }

    @Transactional
    public PlaceConfirmationView confirm(
            Long memberId,
            Long importId,
            List<PlaceSelection> selections
    ) {
        if (selections.stream().map(PlaceSelection::candidateId).distinct().count()
                != selections.size()) {
            throw new InvalidPlaceCandidateException();
        }
        lockActiveMember(memberId);
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(PlaceImportNotFoundException::new);
        if (!placeImport.getMemberId().equals(memberId)) {
            throw new PlaceImportAccessDeniedException();
        }
        Map<Long, PlaceCandidate> candidates = candidateRepository
                .findAllById(selections.stream().map(PlaceSelection::candidateId).toList())
                .stream()
                .collect(Collectors.toMap(PlaceCandidate::getId, Function.identity()));
        validateCandidates(importId, selections, candidates);
        rejectInvalidStateOrDuplicates(memberId, placeImport, selections, candidates);
        ActiveCoupleMember membership = activeCoupleMemberRepository
                .findByMemberId(memberId)
                .orElse(null);
        Long partnerId = partnerId(membership, memberId);
        Instant now = clock.instant();
        List<PlaceConfirmationView.SavedPlaceView> savedPlaces = selections.stream()
                .map(selection -> saveSelection(
                        memberId,
                        importId,
                        selection,
                        candidates.get(selection.candidateId()),
                        membership,
                        partnerId,
                        now
                ))
                .toList();
        placeImport.markCompleted(now);
        importRepository.save(placeImport);
        return new PlaceConfirmationView(
                importId,
                placeImport.getStatus(),
                savedPlaces
        );
    }

    private PlaceConfirmationView.SavedPlaceView saveSelection(
            Long memberId,
            Long importId,
            PlaceSelection selection,
            PlaceCandidate candidate,
            ActiveCoupleMember membership,
            Long partnerId,
            Instant now
    ) {
        Place place = findPlace(candidate.getPlaceId());
        MemberPlace created = memberPlaceRepository.save(MemberPlace.save(
                memberId,
                place,
                importId,
                selection.alias(),
                selection.memo(),
                now
        ));
        publishSavedEvent(membership, memberId, partnerId, place.getId(), now);
        return new PlaceConfirmationView.SavedPlaceView(
                toView(created, place, ownershipStatus(partnerId, place.getId())),
                true
        );
    }

    private void lockActiveMember(Long memberId) {
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
    }

    private void validateCandidates(
            Long importId,
            List<PlaceSelection> selections,
            Map<Long, PlaceCandidate> candidates
    ) {
        boolean invalid = selections.stream()
                .map(selection -> candidates.get(selection.candidateId()))
                .anyMatch(candidate -> candidate == null
                        || !candidate.getImportId().equals(importId)
                        || !isConfirmable(candidate.getVerificationStatus())
                        || candidate.getPlaceId() == null);
        if (invalid) {
            throw new InvalidPlaceCandidateException();
        }
        long distinctPlaceCount = selections.stream()
                .map(selection -> candidates.get(selection.candidateId()).getPlaceId())
                .distinct()
                .count();
        if (distinctPlaceCount != selections.size()) {
            throw new InvalidPlaceCandidateException();
        }
    }

    private boolean isConfirmable(PlaceVerificationStatus status) {
        return status == PlaceVerificationStatus.VERIFIED
                || status == PlaceVerificationStatus.REVIEW_REQUIRED;
    }

    private void rejectInvalidStateOrDuplicates(
            Long memberId,
            PlaceImport placeImport,
            List<PlaceSelection> selections,
            Map<Long, PlaceCandidate> candidates
    ) {
        Set<Long> existingPlaceIds = existingPlaceIds(memberId, selections, candidates);
        if (!existingPlaceIds.isEmpty()) {
            throw new PlaceAlreadySavedException();
        }
        if (placeImport.getStatus() != PlaceImportStatus.REVIEW_REQUIRED) {
            throw new InvalidPlaceCandidateException();
        }
    }

    private Set<Long> existingPlaceIds(
            Long memberId,
            List<PlaceSelection> selections,
            Map<Long, PlaceCandidate> candidates
    ) {
        List<Long> placeIds = selections.stream()
                .map(selection -> candidates.get(selection.candidateId()).getPlaceId())
                .distinct()
                .toList();
        return memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(memberId, placeIds)
                .stream()
                .map(memberPlace -> memberPlace.getPlace().getId())
                .collect(Collectors.toSet());
    }

    private MemberPlaceView toView(
            MemberPlace saved,
            Place place,
            PlaceOwnershipStatus ownershipStatus
    ) {
        return new MemberPlaceView(
                saved.getMemberId(),
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
                place.getCategoryName(),
                ownershipStatus,
                saved.getAlias(),
                saved.getMemo(),
                saved.getSavedAt()
        );
    }

    private Place findPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(PlaceNotFoundException::new);
    }

    private void publishSavedEvent(
            ActiveCoupleMember membership,
            Long memberId,
            Long partnerId,
            Long placeId,
            Instant now
    ) {
        if (membership == null || partnerId == null) {
            return;
        }
        eventPublisher.publishEvent(new ContentSavedEvent(
                membership.getCouple().getId(),
                memberId,
                partnerId,
                placeId,
                now
        ));
    }

    private Long partnerId(ActiveCoupleMember membership, Long memberId) {
        if (membership == null) {
            return null;
        }
        return activeCoupleMemberRepository.findAllByCoupleId(membership.getCouple().getId())
                .stream()
                .map(ActiveCoupleMember::getMemberId)
                .filter(id -> !id.equals(memberId))
                .findFirst()
                .orElse(null);
    }

    private PlaceOwnershipStatus ownershipStatus(Long partnerId, Long placeId) {
        if (partnerId == null) {
            return PlaceOwnershipStatus.MINE;
        }
        return memberPlaceRepository.findByMemberIdAndPlaceId(partnerId, placeId).isPresent()
                ? PlaceOwnershipStatus.TOGETHER
                : PlaceOwnershipStatus.MINE;
    }

    public record PlaceSelection(
            Long candidateId,
            String alias,
            String memo
    ) {
    }
}
