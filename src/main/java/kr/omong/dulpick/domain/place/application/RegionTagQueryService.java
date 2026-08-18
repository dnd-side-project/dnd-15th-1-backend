package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.RegionTagNotFoundException;
import kr.omong.dulpick.domain.place.domain.PlaceRegionTag;
import kr.omong.dulpick.domain.place.domain.PlaceRegionTagRepository;
import kr.omong.dulpick.domain.place.domain.RegionTag;
import kr.omong.dulpick.domain.place.domain.RegionTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RegionTagQueryService {

    private final RegionTagRepository regionTagRepository;
    private final PlaceRegionTagRepository placeRegionTagRepository;

    public RegionTagQueryService(
            RegionTagRepository regionTagRepository,
            PlaceRegionTagRepository placeRegionTagRepository
    ) {
        this.regionTagRepository = regionTagRepository;
        this.placeRegionTagRepository = placeRegionTagRepository;
    }

    @Transactional(readOnly = true)
    public List<RegionTagView> getAll() {
        return regionTagRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(tag -> toView(
                        tag,
                        placeRegionTagRepository.countByRegionTagId(tag.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegionTagView get(Long regionTagId) {
        RegionTag tag = requireActiveTag(regionTagId);
        return toView(tag, placeRegionTagRepository.countByRegionTagId(tag.getId()));
    }

    @Transactional(readOnly = true)
    public RegionTagSummaryView getSummary(Long regionTagId) {
        return toSummary(requireActiveTag(regionTagId));
    }

    @Transactional(readOnly = true)
    public List<RegionTagSummaryView> getActiveSummaries() {
        return regionTagRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<RegionTagSummaryView>> getTagsByPlaceIds(
            Collection<Long> placeIds
    ) {
        List<Long> distinctPlaceIds = placeIds.stream().distinct().toList();
        if (distinctPlaceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RegionTagSummaryView> activeTags = getActiveSummaries().stream()
                .collect(Collectors.toMap(
                        RegionTagSummaryView::regionTagId,
                        Function.identity()
                ));
        return placeRegionTagRepository.findAllByPlaceIdIn(distinctPlaceIds)
                .stream()
                .filter(link -> activeTags.containsKey(link.getRegionTagId()))
                .collect(Collectors.groupingBy(
                        PlaceRegionTag::getPlaceId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                link -> activeTags.get(link.getRegionTagId()),
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        tags -> tags.stream()
                                                .sorted(Comparator.comparingInt(
                                                        RegionTagSummaryView::displayOrder
                                                ))
                                                .toList()
                                )
                        )
                ));
    }

    public List<RegionTagSummaryView> matchingTags(
            String address,
            String roadAddress,
            List<RegionTagSummaryView> activeTags
    ) {
        return activeTags.stream()
                .filter(tag -> matchesAddress(tag, address, roadAddress))
                .toList();
    }

    public boolean matchesAddress(
            RegionTagSummaryView tag,
            String address,
            String roadAddress
    ) {
        String normalizedAddress = normalize(
                (address == null ? "" : address) + (roadAddress == null ? "" : roadAddress)
        );
        return normalizedAddress.contains(normalize(tag.name()));
    }

    private RegionTag requireActiveTag(Long regionTagId) {
        return regionTagRepository.findByIdAndActiveTrue(regionTagId)
                .orElseThrow(RegionTagNotFoundException::new);
    }

    private RegionTagSummaryView toSummary(RegionTag tag) {
        return new RegionTagSummaryView(tag.getId(), tag.getName(), tag.getDisplayOrder());
    }

    private RegionTagView toView(RegionTag tag, long placeCount) {
        return new RegionTagView(
                tag.getId(),
                tag.getName(),
                tag.getDisplayOrder(),
                placeCount
        );
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
