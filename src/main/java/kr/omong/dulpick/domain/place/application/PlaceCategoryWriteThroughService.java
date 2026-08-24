package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Kakao에서 확인된 분류 정보 중 비어 있는 장소 분류만 보완한다.
 * 조회 트랜잭션이 read-only인 경우에도 독립된 쓰기 트랜잭션으로 반영한다.
 */
@Service
public class PlaceCategoryWriteThroughService {

    private final PlaceRepository placeRepository;
    private final Clock clock;

    public PlaceCategoryWriteThroughService(
            PlaceRepository placeRepository,
            Clock clock
    ) {
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fillIfMissing(
            Long placeId,
            String storedCategoryGroupCode,
            String storedCategory,
            String kakaoCategoryGroupCode,
            String kakaoCategory
    ) {
        if (!shouldFill(
                storedCategoryGroupCode,
                storedCategory,
                kakaoCategoryGroupCode,
                kakaoCategory
        )) {
            return;
        }
        placeRepository.updateCategoryIfMissing(
                placeId,
                kakaoCategoryGroupCode,
                kakaoCategory,
                Instant.now(clock)
        );
    }

    private boolean shouldFill(
            String storedCategoryGroupCode,
            String storedCategory,
            String kakaoCategoryGroupCode,
            String kakaoCategory
    ) {
        return DulpickPlaceCategory.isFallback(storedCategoryGroupCode, storedCategory)
                && !DulpickPlaceCategory.isFallback(kakaoCategoryGroupCode, kakaoCategory);
    }
}
