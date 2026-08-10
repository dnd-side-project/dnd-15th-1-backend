package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.util.List;

public record PlaceConfirmationView(
        Long importId,
        PlaceImportStatus status,
        List<SavedPlaceView> savedPlaces
) {

    public record SavedPlaceView(
            MemberPlaceView place,
            boolean newlySaved
    ) {
    }
}
