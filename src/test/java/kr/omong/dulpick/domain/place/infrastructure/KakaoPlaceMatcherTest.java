package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.ExtractedPlace;
import kr.omong.dulpick.domain.place.application.PlaceSearchResult;
import kr.omong.dulpick.domain.place.domain.PlaceVerificationStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoPlaceMatcherTest {

    private final KakaoPlaceMatcher matcher = new KakaoPlaceMatcher();

    @Test
    void treatsDistanceDescriptionAsOptionalHintWhenNameMatches() {
        ExtractedPlace extracted = extracted(
                "에이미미버터",
                "의정부역 5분 거리"
        );

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "에이미미버터",
                "경기 의정부시 의정부동 1",
                "경기 의정부시 시민로 1"
        )));

        assertThat(result).hasValueSatisfying(match ->
                assertThat(match.status()).isEqualTo(PlaceVerificationStatus.VERIFIED)
        );
    }

    @Test
    void returnsChangedBusinessNameAtExactAddressForReview() {
        ExtractedPlace extracted = extracted(
                "시어풀빌라",
                "경기 가평군 설악면 유명로 100"
        );

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "시어팬션",
                "경기 가평군 설악면 선촌리 10",
                "경기 가평군 설악면 유명로 100"
        )));

        assertThat(result).hasValueSatisfying(match -> {
            assertThat(match.place().name()).isEqualTo("시어팬션");
            assertThat(match.status()).isEqualTo(PlaceVerificationStatus.REVIEW_REQUIRED);
        });
    }

    @Test
    void returnsAliasWithMatchingLocationForReview() {
        ExtractedPlace extracted = extracted("MIP 라운지", "잠실");

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "밉",
                "서울 송파구 잠실동 100",
                "서울 송파구 올림픽로 100"
        )));

        assertThat(result).hasValueSatisfying(match ->
                assertThat(match.status()).isEqualTo(PlaceVerificationStatus.REVIEW_REQUIRED)
        );
    }

    @Test
    void rejectsSameRegionAndPartialNameWhenPreciseAddressDiffers() {
        ExtractedPlace extracted = extracted(
                "히든밸리",
                "경기 양평군 서종면 수대울길 139-30"
        );

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "히든밸리켄넬",
                "경기 양평군 서종면 수입리 425-9",
                "경기 양평군 서종면 수대울길187번길 83"
        )));

        assertThat(result).isEmpty();
    }

    @Test
    void prioritizesSameRoadAsReviewCandidateOverDifferentRoad() {
        ExtractedPlace extracted = extracted(
                "을지식당",
                "서울 중구 을지로40길 17"
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

        assertThat(result).hasValueSatisfying(match -> {
            assertThat(match.place()).isEqualTo(sameRoadResult);
            assertThat(match.status()).isEqualTo(PlaceVerificationStatus.REVIEW_REQUIRED);
        });
    }

    @Test
    void rejectsSameRoadNameInDifferentProvince() {
        ExtractedPlace extracted = extracted(
                "중앙식당",
                "부산 중구 중앙대로 10"
        );

        var result = matcher.findBest(extracted, List.of(place(
                "1",
                "중앙식당",
                "서울 중구 중앙동 1",
                "서울 중구 중앙대로 10"
        )));

        assertThat(result).isEmpty();
    }

    private ExtractedPlace extracted(String name, String addressHint) {
        return new ExtractedPlace(
                name,
                addressHint,
                name + " " + addressHint,
                "EXPLICIT_VENUE"
        );
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
