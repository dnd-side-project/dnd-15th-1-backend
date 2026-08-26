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

    public PlaceQueryService(
            MemberPlaceRepository memberPlaceRepository,
            ActiveCoupleMemberRepository activeCoupleMemberRepository
    ) {
        this.memberPlaceRepository = memberPlaceRepository;
        this.activeCoupleMemberRepository = activeCoupleMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(Long memberId) {
        List<List<MemberPlace>> groupedPlaces = groupedVisiblePlaces(memberId);
        Map<Long, Long> saveCounts = saveCountsFor(groupedPlaces);
        return groupedPlaces.stream()
                .map(places -> toView(memberId, places, saveCounts))
                .sorted(Comparator.comparing(MemberPlaceView::savedAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPlaceView> getVisiblePlaces(
            Long memberId,
            DulpickPlaceCategory category,
            PlaceOwnershipStatus ownershipStatus
    ) {
        List<List<MemberPlace>> groupedPlaces = groupedVisiblePlaces(memberId);
        Map<Long, Long> saveCounts = saveCountsFor(groupedPlaces);
        return groupedPlaces.stream()
                .filter(places -> category == null
                        || category.getDisplayName().equals(places.getFirst().getPlace().getCategoryName()))
                .filter(places -> ownership(memberId, places).matchesFilter(ownershipStatus))
                .map(places -> toView(memberId, places, saveCounts))
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
                        entry -> ownership(memberId, entry.getValue())
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

    private MemberPlaceView toView(
            Long memberId,
            List<MemberPlace> places,
            Map<Long, Long> saveCounts
    ) {
        MemberPlace mine = places.stream()
                .filter(place -> place.getMemberId().equals(memberId))
                .findFirst()
                .orElse(null);
        MemberPlace selected = mine == null ? places.getFirst() : mine;
        PlaceOwnership ownership = ownership(memberId, places);
        Long placeId = selected.getPlace().getId();
        return new MemberPlaceView(
                selected.getMemberId(),
                placeId,
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
                selected.getPlace().getImageUrls(),
                saveCounts.getOrDefault(placeId, 0L).intValue()
        );
    }

    private Map<Long, Long> saveCountsFor(List<List<MemberPlace>> groupedPlaces) {
        return savedMemberCounts(groupedPlaces.stream()
                .map(places -> places.getFirst().getPlace().getId())
                .toList());
    }

    @Transactional(readOnly = true)
    public int savedMemberCount(Long placeId) {
        if (placeId == null) {
            return 0;
        }
        return savedMemberCounts(List.of(placeId)).getOrDefault(placeId, 0L).intValue();
    }

    private Map<Long, Long> savedMemberCounts(Collection<Long> placeIds) {
        List<Long> distinctPlaceIds = placeIds.stream().distinct().toList();
        if (distinctPlaceIds.isEmpty()) {
            return Map.of();
        }
        return memberPlaceRepository.countSavesByPlaceIdIn(distinctPlaceIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private PlaceOwnership ownership(
            Long memberId,
            List<MemberPlace> places
    ) {
        boolean savedByMe = places.stream().anyMatch(place -> place.getMemberId().equals(memberId));
        boolean savedByPartner = places.stream().anyMatch(place -> !place.getMemberId().equals(memberId));
        return PlaceOwnership.of(savedByMe, savedByPartner);
    }
}
