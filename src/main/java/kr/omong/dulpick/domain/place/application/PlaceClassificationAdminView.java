package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;

public record PlaceClassificationAdminView(
        Long placeId,
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        String categoryName,
        String thumbnailUrl,
        PlaceClassificationStatus status,
        AxisView<PlaceEnvironment> environment,
        AxisView<PlaceActivity> activity,
        AxisView<PlaceTime> time,
        AxisView<PlaceFocus> focus
) {

    public static PlaceClassificationAdminView from(Place place, PlaceClassification classification) {
        return new PlaceClassificationAdminView(
                place.getId(),
                place.getKakaoPlaceId(),
                place.getName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getCategoryName(),
                place.getThumbnailUrl(),
                classification == null
                        ? PlaceClassificationStatus.UNCLASSIFIED
                        : classification.getStatus(),
                AxisView.of(
                        classification == null ? null : classification.getEnvironment(),
                        classification == null ? null : classification.getEnvironmentSource()
                ),
                AxisView.of(
                        classification == null ? null : classification.getActivity(),
                        classification == null ? null : classification.getActivitySource()
                ),
                AxisView.of(
                        classification == null ? null : classification.getTime(),
                        classification == null ? null : classification.getTimeSource()
                ),
                AxisView.of(
                        classification == null ? null : classification.getFocus(),
                        classification == null ? null : classification.getFocusSource()
                )
        );
    }

    public record AxisView<T>(
            T value,
            ClassificationSource source
    ) {

        private static <T> AxisView<T> of(T value, ClassificationSource source) {
            return new AxisView<>(value, source);
        }
    }
}
