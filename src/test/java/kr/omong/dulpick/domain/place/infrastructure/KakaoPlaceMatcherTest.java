package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoPlaceMatcherTest {

    private final KakaoPlaceMatcher matcher = new KakaoPlaceMatcher();

    @Test
    void prioritizesSameRoadOverSameNameSearchOrder() {
        ExtractedPlace extracted = new ExtractedPlace(
                "을지식당",
                "서울 중구 을지로40길 17",
                "을지식당 서울 중구 을지로40길 17",
                "EXPLICIT_VENUE"
        );
        PlaceSearchResult wrongTopResult = place(
                "1",
                "을지식당",
                "서울 중구 을지로3가 296-2",
                "서울 중구 충무로 47"
        );
        PlaceSearchResult sameRoadResult = place(
                "2",
                "을지식당",
                "서울 중구 을지로6가 67-1",
                "서울 중구 을지로40길 15"
        );

        var result = matcher.findBest(extracted, List.of(wrongTopResult, sameRoadResult));

        assertThat(result).contains(sameRoadResult);
    }

    @Test
    void rejectsSameNameWhenAddressHintDoesNotMatch() {
        ExtractedPlace extracted = new ExtractedPlace(
                "을지식당",
                "서울 중구 을지로40길 17",
                "을지식당 서울 중구 을지로40길 17",
                "EXPLICIT_VENUE"
        );

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "을지식당",
                "서울 중구 을지로3가 296-2",
                "서울 중구 충무로 47"
        )));

        assertThat(result).isEmpty();
    }

    private PlaceSearchResult place(
            String id,
            String name,
            String address,
            String roadAddress
    ) {
        return new PlaceSearchResult(
                id,
                name,
                address,
                roadAddress,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "FD6",
                "음식점 > 한식",
                null
        );
    }
}
