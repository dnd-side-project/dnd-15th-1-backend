package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.RegionTagView;

public record RegionTagResponse(
        @Schema(description = "지역 태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long regionTagId,
        @Schema(description = "지역 태그명", example = "성수", requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @Schema(description = "화면 표시 순서", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int displayOrder,
        @Schema(description = "현재 연결된 공용 장소 수", example = "24", requiredMode = Schema.RequiredMode.REQUIRED)
        long placeCount
) {

    public static RegionTagResponse from(RegionTagView view) {
        return new RegionTagResponse(
                view.regionTagId(),
                view.name(),
                view.displayOrder(),
                view.placeCount()
        );
    }
}
