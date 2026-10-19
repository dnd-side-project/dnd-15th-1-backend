package kr.omong.dulpick.domain.date.presentation.dto;

import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseView;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.domain.date.presentation.dto.response.DateCourseResponse;
import kr.omong.dulpick.domain.place.application.PlaceDateTraitsView;
import kr.omong.dulpick.domain.place.application.RoutePoint;
import kr.omong.dulpick.domain.place.application.TransitLeg;
import kr.omong.dulpick.domain.place.application.TransitRoute;
import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DateCourseResponseTest {

    @Test
    void mapsWalkPathTransitLegsAndDateTraits() {
        TransitRoute transit = new TransitRoute(
                "BUS_AND_SUBWAY", 5200, 2700, 1, 1550,
                List.of(new TransitLeg(
                        "SUBWAY", "2호선 (성수 > 강남)", 4100, 1800, "2호선", "지하철",
                        List.of("성수", "강남"),
                        List.of(new RoutePoint(37.5445, 127.056))
                ))
        );
        WalkingRoute walk = new WalkingRoute(
                4025, 3914, List.of(new RoutePoint(37.5445, 127.056), new RoutePoint(37.548, 127.041)), transit
        );
        PlaceDateTraitsView traits = new PlaceDateTraitsView(
                PlaceClassificationStatus.CLASSIFIED,
                PlaceEnvironment.INDOOR, PlaceActivity.STATIC, PlaceTime.DAY, PlaceFocus.FOOD
        );

        DateCourseResponse response = DateCourseResponse.from(new DateCourseView(
                1L, "성수동 데이트", Instant.parse("2026-08-16T10:30:00Z"), DateCourseStatus.CONFIRMED, 0, 2,
                List.of(
                        place(1, walk, traits),
                        place(2, null, null)
                )
        ));

        DateCourseResponse.DateCoursePlaceResponse first = response.places().getFirst();
        assertThat(first.walkToNext().distanceMeters()).isEqualTo(4025);
        assertThat(first.walkToNext().path()).hasSize(2);
        assertThat(first.walkToNext().path().getFirst().latitude()).isEqualTo(37.5445);
        DateCourseResponse.TransitToNextResponse mapped = first.walkToNext().transit();
        assertThat(mapped.type()).isEqualTo("BUS_AND_SUBWAY");
        assertThat(mapped.durationSeconds()).isEqualTo(2700);
        assertThat(mapped.transfers()).isEqualTo(1);
        assertThat(mapped.fare()).isEqualTo(1550);
        assertThat(mapped.legs()).hasSize(1);
        assertThat(mapped.legs().getFirst().vehicleName()).isEqualTo("2호선");
        assertThat(mapped.legs().getFirst().stops()).containsExactly("성수", "강남");
        assertThat(mapped.legs().getFirst().path()).hasSize(1);
        assertThat(first.dateTraits().status()).isEqualTo(PlaceClassificationStatus.CLASSIFIED);
        assertThat(first.dateTraits().environment()).isEqualTo(PlaceEnvironment.INDOOR);
        assertThat(first.dateTraits().focus()).isEqualTo(PlaceFocus.FOOD);

        DateCourseResponse.DateCoursePlaceResponse last = response.places().get(1);
        assertThat(last.walkToNext()).isNull();
        assertThat(last.dateTraits().status()).isEqualTo(PlaceClassificationStatus.UNCLASSIFIED);
    }

    @Test
    void walkWithoutTransitHasNullTransit() {
        DateCourseResponse response = DateCourseResponse.from(new DateCourseView(
                1L, "코스", Instant.parse("2026-08-16T10:30:00Z"), DateCourseStatus.DRAFT, 0, 1,
                List.of(place(1, new WalkingRoute(400, 300), null))
        ));

        assertThat(response.places().getFirst().walkToNext().transit()).isNull();
        assertThat(response.places().getFirst().walkToNext().path()).isEmpty();
    }

    private DateCoursePlaceView place(int order, WalkingRoute walk, PlaceDateTraitsView traits) {
        return new DateCoursePlaceView(
                order, (long) order, "장소" + order, "주소", "도로명 주소",
                new BigDecimal("37.5445"), new BigDecimal("127.056"),
                "음식점 > 카페", "카페", null, List.of(), walk, traits
        );
    }
}
