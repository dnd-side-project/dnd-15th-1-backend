package kr.omong.dulpick.domain.place;

import kr.omong.dulpick.domain.place.application.RegionTagAssignmentService;
import kr.omong.dulpick.domain.place.application.RegionTagQueryService;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RegionTagIntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private RegionTagAssignmentService assignmentService;

    @Autowired
    private RegionTagQueryService queryService;

    @Test
    void seededTagIsLinkedToNewPlaceByAddressName() {
        Instant now = Instant.now();
        Place place = placeRepository.save(Place.create(
                "region-" + UUID.randomUUID(),
                "성수 테스트 카페",
                "서울특별시 성동구 성수동1가",
                "서울특별시 성동구 성수이로",
                new BigDecimal("37.5446000"),
                new BigDecimal("127.0557000"),
                "음식점 > 카페",
                "CE7",
                null,
                now
        ));

        assignmentService.assignMatchingTags(place, now);

        var tagsByPlace = queryService.getTagsByPlaceIds(List.of(place.getId()));
        assertThat(tagsByPlace.get(place.getId()))
                .extracting("name")
                .containsExactly("성수");
    }
}
