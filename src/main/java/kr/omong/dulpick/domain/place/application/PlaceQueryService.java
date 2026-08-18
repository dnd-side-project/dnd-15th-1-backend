package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlaceQueryService {

    private final MemberPlaceRepository memberPlaceRepository;
    private final ActiveCoupleMemberRepository activeCoupleMemberRepository;
    private final RegionTagQueryService regionTagQueryService;

    public PlaceQueryService(
            MemberPlaceRepository memberPlaceRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository,
            RegionTagQueryService regionTagQueryService
    ) {
        this.memberPlaceRepository = memberPlaceRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
        this.regionTagQueryService = regionTagQueryService;
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(Long memberId) {
        return groupedVisiblePlaces(memberId).stream()
                .map(places -> toView(memberId, places))
                .sorted(Comparator.comparing(MemberPlaceView::savedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(
            Long memberId,
            DulpickPlaceCategory category,
            PlaceOwnershipStatus ownershipStatus,
            Long regionTagId
    ) {
        RegionTagSummaryView selectedRegionTag = regionTagId == null
                ? null
                : regionTagQueryService.getSummary(regionTagId);
        boolean hasActiveCouple = hasActiveCouple(memberId);
        List<List<MemberPlace>> groupedPlaces = groupedVisiblePlaces(memberId);
        Map<Long, List<RegionTagSummaryView>> tagsByPlaceId = selectedRegionTag == null
                ? Map.of()
                : regionTagQueryService.getTagsByPlaceIds(
                groupedPlaces.stream()
                        .map(places -> places.getFirst().getPlace().getId())
                        .toList()
        );
        return groupedPlaces.stream()
                .filter(places -> category == null
                        || category.getDisplayName().equals(places.getFirst().getPlace().getCategoryName()))
                .filter(places -> ownership(memberId, places, hasActiveCouple)
                        .matchesFilter(ownershipStatus))
                .filter(places -> selectedRegionTag == null
                        || tagsByPlaceId.getOrDefault(
                                places.getFirst().getPlace().getId(),
                                List.of()
                        ).stream()
                        .anyMatch(tag -> tag.regionTagId().equals(
                                selectedRegionTag.regionTagId()
                        )))
                .map(places -> toView(memberId, places))
                .sorted(Comparator.comparing(MemberPlaceView::savedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, PlaceOwnership> getOwnerships(
            Long memberId,
            Collection<Long> placeIds
    ) {
        List<Long> distinctPlaceIds = placeIds.stream().distinct().toList();
        if (distinctPlaceIds.isEmpty()) {
            return Map.of();
        }
        boolean hasActiveCouple = hasActiveCouple(memberId);
        return memberPlaceRepository.findAllByMemberIdInAndPlaceIdIn(
                        visibleMemberIds(memberId),
                        distinctPlaceIds
                )
                .stream()
                .collect(Collectors.groupingBy(
                        memberPlace -> memberPlace.getPlace().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> ownership(memberId, entry.getValue(), hasActiveCouple)
                ));
    }

    private List<List<MemberPlace>> groupedVisiblePlaces(Long memberId) {
        return memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(
                        visibleMemberIds(memberId)
                )
                .stream()
                .collect(Collectors.groupingBy(
                        memberPlace -> memberPlace.getPlace().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .toList();
    }

    private List<Long> visibleMemberIds(Long memberId) {
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
        return memberIds;
    }

    private boolean hasActiveCouple(Long memberId) {
        return visibleMemberIds(memberId).size() > 1;
    }

    private MemberPlaceView toView(Long memberId, List<MemberPlace> places) {
        MemberPlace mine = places.stream()
                .filter(place -> place.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);
        MemberPlace selected = mine == null ? places.getFirst() : mine;
        PlaceOwnership ownership = ownership(memberId, places, hasActiveCouple(memberId));
        return new MemberPlaceView(
                selected.getMemberId(),
                selected.getPlace().getId(),
                selected.getPlace().getKakaoPlaceId(),
                selected.getPlace().getName(),
                selected.getPlace().getAddress(),
                selected.getPlace().getRoadAddress(),
                selected.getPlace().getLatitude(),
                selected.getPlace().getLongitude(),
                selected.getPlace().getCategory(),
                selected.getPlace().getCategoryName(),
                ownership.status(),
                selected.getAlias(),
                selected.getSavedAt(),
                selected.getPlace().getThumbnailUrl(),
                selected.getPlace().getImageUrls()
        );
    }

    private PlaceOwnership ownership(
            Long memberId,
            List<MemberPlace> places,
            boolean hasActiveCouple
    ) {
        boolean savedByMe = places.stream().anyMatch(place -> place.getMemberId().equals(memberId));
        boolean savedByPartner = places.stream().anyMatch(place -> !place.getMemberId().equals(memberId));
        return PlaceOwnership.of(hasActiveCouple, savedByMe, savedByPartner);
    }
}
