package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlaceSaveDeleteResponse(
        @Schema(description = "저장 관계 삭제 성공 여부입니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean deleted,
        @Schema(description = "저장을 해제한 공용 장소 ID입니다.", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
        Long placeId
) {
}
