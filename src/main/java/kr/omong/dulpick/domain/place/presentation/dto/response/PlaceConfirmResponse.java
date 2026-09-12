package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceConfirmationView;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.util.List;

public record PlaceConfirmResponse(
        @Schema(description = "후보 선택을 완료한 회원별 장소 분석 작업 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long importId,
        @Schema(
                description = "후보 저장 처리 후 분석 작업 상태입니다. RECEIVED, PROCESSING, REVIEW_REQUIRED, COMPLETED, FAILED 중 하나입니다.",
                allowableValues = {"RECEIVED", "PROCESSING", "REVIEW_REQUIRED", "COMPLETED", "FAILED"},
                example = "COMPLETED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PlaceImportStatus status,
        @Schema(description = "선택한 장소 목록입니다. 이미 저장된 장소도 포함하며 newlySaved=false로 표시합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
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
            @Schema(description = "저장된 장소의 상세 정보와 저장 관계입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
            MemberPlaceResponse place,
            @Schema(description = "이번 확정 요청에서 새로 저장된 장소이면 true입니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
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
