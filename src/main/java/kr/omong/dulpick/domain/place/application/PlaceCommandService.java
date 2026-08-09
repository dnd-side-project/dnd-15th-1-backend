package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.domain.notification.application.event.ContentSavedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        Place place = placeRepository.findByKakaoPlaceId(searchResult.kakaoPlaceId())
                .orElseGet(() -> placeRepository.save(Place.create(
                        searchResult.kakaoPlaceId(),
                        searchResult.name(),
                        searchResult.address(),
                        searchResult.roadAddress(),
                        searchResult.latitude(),
                        searchResult.longitude(),
                        searchResult.category(),
                        searchResult.thumbnailUrl(),
                        clock.instant()
                )));
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
        return toView(saved, place);
    }

    @Transactional
    public List<MemberPlaceView> confirm(
            Long memberId,
            Long importId,
            List<PlaceSelection> selections
    ) {
        if (selections.stream().map(PlaceSelection::candidateId).distinct().count()
                != selections.size()) {
            throw new InvalidPlaceCandidateException();
        }
        PlaceImport placeImport = importRepository.findById(importId)
                .orElseThrow(PlaceImportNotFoundException::new);
        if (!placeImport.getMemberId().equals(memberId)) {
            throw new PlaceImportAccessDeniedException();
        }
        if (placeImport.getStatus() != PlaceImportStatus.REVIEW_REQUIRED) {
            throw new InvalidPlaceCandidateException();
        }
        Member member = memberRepository.findForUpdateById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        Map<Long, PlaceCandidate> candidates = candidateRepository
                .findAllById(selections.stream().map(PlaceSelection::candidateId).toList())
                .stream()
                .collect(Collectors.toMap(PlaceCandidate::getId, Function.identity()));
        ActiveCoupleMember membership = activeCoupleMemberRepository
                .findByMemberId(memberId)
                .orElse(null);
        Long partnerId = partnerId(membership, memberId);
        Instant now = clock.instant();
        List<MemberPlaceView> savedPlaces = selections.stream()
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
        return savedPlaces;
    }

    private MemberPlaceView saveSelection(
            Long memberId,
            Long importId,
            PlaceSelection selection,
            PlaceCandidate candidate,
            ActiveCoupleMember membership,
            Long partnerId,
            Instant now
    ) {
        if (candidate == null
                || !candidate.getImportId().equals(importId)
                || candidate.getVerificationStatus() != PlaceVerificationStatus.VERIFIED
                || candidate.getPlaceId() == null) {
            throw new InvalidPlaceCandidateException();
        }
        Place place = findPlace(candidate.getPlaceId());
        MemberPlace saved = memberPlaceRepository.findByMemberIdAndPlaceId(memberId, place.getId())
                .orElseGet(() -> {
                    MemberPlace created = memberPlaceRepository.save(MemberPlace.save(
                            memberId,
                            place,
                            importId,
                            selection.alias(),
                            selection.memo(),
                            now
                    ));
                    publishSavedEvent(membership, memberId, partnerId, place.getId(), now);
                    return created;
                });
        return toView(saved, place);
    }

    private MemberPlaceView toView(MemberPlace saved, Place place) {
        return new MemberPlaceView(
                saved.getMemberId(),
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCategory(),
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

    public record PlaceSelection(
            Long candidateId,
            String alias,
            String memo
    ) {
    }
}
