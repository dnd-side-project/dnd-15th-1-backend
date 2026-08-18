package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;

public record PlaceDateTraitsView(
        PlaceClassificationStatus status,
        PlaceEnvironment environment,
        PlaceActivity activity,
        PlaceTime time,
        PlaceFocus focus
) {

    public static PlaceDateTraitsView unclassified() {
        return new PlaceDateTraitsView(
                PlaceClassificationStatus.UNCLASSIFIED,
                null,
                null,
                null,
                null
        );
    }

    public static PlaceDateTraitsView from(PlaceClassification classification) {
        if (classification == null) {
            return unclassified();
        }
        return new PlaceDateTraitsView(
                classification.getStatus(),
                classification.getEnvironment(),
                classification.getActivity(),
                classification.getTime(),
                classification.getFocus()
        );
    }
}
