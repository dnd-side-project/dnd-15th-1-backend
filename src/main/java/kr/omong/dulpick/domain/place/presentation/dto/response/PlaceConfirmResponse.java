package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceConfirmationView;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.util.List;

public record PlaceConfirmResponse(
        @Schema(description = "후보 선택을 완료한 회원별 장소 분석 작업 ID")
        Long importId,
        PlaceImportStatus status,
        List<SavedPlaceResponse> savedPlaces
) {

    public static PlaceConfirmResponse from(PlaceConfirmationView view) {
        return new PlaceConfirmResponse(
                view.importId(),
                view.status(),
                view.savedPlaces().stream().map(SavedPlaceResponse::from).toList()
        );
    }

    public record SavedPlaceResponse(
            MemberPlaceResponse place,
            boolean newlySaved
    ) {

        private static SavedPlaceResponse from(PlaceConfirmationView.SavedPlaceView view) {
            return new SavedPlaceResponse(
                    MemberPlaceResponse.from(view.place()),
                    view.newlySaved()
            );
        }
    }
}
