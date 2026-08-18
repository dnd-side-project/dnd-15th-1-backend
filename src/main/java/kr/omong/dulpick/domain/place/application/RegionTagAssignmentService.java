package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRegionTagRepository;
import kr.omong.dulpick.domain.place.domain.RegionTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class RegionTagAssignmentService {

    private final RegionTagRepository regionTagRepository;
    private final PlaceRegionTagRepository placeRegionTagRepository;

    public RegionTagAssignmentService(
            RegionTagRepository regionTagRepository,
            PlaceRegionTagRepository placeRegionTagRepository
    ) {
        this.regionTagRepository = regionTagRepository;
        this.placeRegionTagRepository = placeRegionTagRepository;
    }

    @Transactional
    public void assignMatchingTags(Place place, Instant now) {
        String normalizedAddress = normalize(
                (place.getAddress() == null ? "" : place.getAddress())
                        + (place.getRoadAddress() == null ? "" : place.getRoadAddress())
        );
        regionTagRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .filter(tag -> normalizedAddress.contains(normalize(tag.getName())))
                .forEach(tag -> placeRegionTagRepository.insertIfAbsent(
                        place.getId(),
                        tag.getId(),
                        now
                ));
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
