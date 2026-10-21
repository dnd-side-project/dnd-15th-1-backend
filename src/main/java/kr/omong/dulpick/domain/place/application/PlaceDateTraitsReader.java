package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 장소에 저장된 네 축 분류를 placeId 목록 단위로 한 번에 조회합니다.
 */
@Component
public class PlaceDateTraitsReader {

    private final PlaceClassificationRepository placeClassificationRepository;

    public PlaceDateTraitsReader(PlaceClassificationRepository placeClassificationRepository) {
        this.placeClassificationRepository = placeClassificationRepository;
    }

    public Map<Long, PlaceDateTraitsView> findByPlaceIds(Collection<Long> placeIds) {
        List<Long> ids = placeIds == null
                ? List.of()
                : placeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return placeClassificationRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        PlaceClassification::getPlaceId,
                        PlaceDateTraitsView::from,
                        (left, right) -> left
                ));
    }

    public PlaceDateTraitsView findByPlaceId(Long placeId) {
        if (placeId == null) {
            return PlaceDateTraitsView.unclassified();
        }
        return placeClassificationRepository.findById(placeId)
                .map(PlaceDateTraitsView::from)
                .orElseGet(PlaceDateTraitsView::unclassified);
    }

    public static Function<Long, PlaceDateTraitsView> lookup(Map<Long, PlaceDateTraitsView> traits) {
        return placeId -> traits.getOrDefault(placeId, PlaceDateTraitsView.unclassified());
    }
}
