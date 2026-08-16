package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaceQueryService {

    private final MemberPlaceRepository memberPlaceRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;

    public PlaceQueryService(
            MemberPlaceRepository memberPlaceRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository
    ) {
        this.memberPlaceRepository = memberPlaceRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(Long memberId) {
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(memberId);
        activeCoupleMemberRepository.findByMemberId(memberId)
                .map(ActiveCoupleMember::getCouple)
                .map(couple -> activeCoupleMemberRepository.findAllByCoupleId(couple.getId()))
                .ifPresent(members -> members.stream()
                        .map(ActiveCoupleMember::getMemberId)
                        .filter(id -> !id.equals(memberId))
                        .findFirst()
                        .ifPresent(memberIds::add));
        return memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(memberIds)
                .stream()
                .collect(Collectors.groupingBy(
                        memberPlace -> memberPlace.getPlace().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(places -> toView(memberId, places))
                .sorted(Comparator.comparing(MemberPlaceView::savedAt).reversed())
                .toList();
    }

    private MemberPlaceView toView(Long memberId, List<MemberPlace> places) {
        MemberPlace mine = places.stream()
                .filter(place -> place.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);
        MemberPlace selected = mine == null ? places.getFirst() : mine;
        return new MemberPlaceView(
                selected.getMemberId(),
                selected.getPlace().getId(),
                selected.getPlace().getName(),
                selected.getPlace().getAddress(),
                selected.getPlace().getRoadAddress(),
                selected.getPlace().getLatitude(),
                selected.getPlace().getLongitude(),
                selected.getPlace().getCategory(),
                selected.getPlace().getCategoryName(),
                ownershipStatus(memberId, places),
                selected.getAlias(),
                selected.getSavedAt(),
                selected.getPlace().getThumbnailUrl(),
                selected.getPlace().getImageUrls()
        );
    }

    private PlaceOwnershipStatus ownershipStatus(Long memberId, List<MemberPlace> places) {
        boolean savedByMe = places.stream().anyMatch(place -> place.getMemberId().equals(memberId));
        boolean savedByPartner = places.stream().anyMatch(place -> !place.getMemberId().equals(memberId));
        if (savedByMe && savedByPartner) {
            return PlaceOwnershipStatus.TOGETHER;
        }
        return savedByMe ? PlaceOwnershipStatus.MINE : PlaceOwnershipStatus.PARTNER;
    }
}
